package com.atlas.searchengine.service;

import com.atlas.searchengine.model.Document;
import com.atlas.searchengine.model.DocumentRepository;
import com.atlas.searchengine.model.DocumentWriteRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class DocumentWriterServiceTest {

    @Autowired
    private DocumentWriteQueue writeQueue;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentWriterService documentWriterService;

    @BeforeEach
    public void setup() {
        documentRepository.deleteAll();
    }

    @AfterEach
    public void teardown() {
        documentRepository.deleteAll();
    }

    @Test
    public void testConcurrentWritesAreSerializedAndSaved() throws InterruptedException {
        int numberOfThreads = 10;
        int requestsPerThread = 10;
        int totalRequests = numberOfThreads * requestsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                for (int j = 0; j < requestsPerThread; j++) {
                    DocumentWriteRequest req = new DocumentWriteRequest(
                            UUID.randomUUID().toString(),
                            "Title " + threadNum + "-" + j,
                            "Text " + threadNum + "-" + j,
                            "test_collection",
                            "http://test.com/" + threadNum + "/" + j,
                            LocalDateTime.now(),
                            LocalDateTime.now()
                    );
                    writeQueue.enqueue(req);
                }
                latch.countDown();
            });
        }

        // Wait for all producers to finish enqueueing
        boolean finished = latch.await(10, TimeUnit.SECONDS);
        assertTrue(finished, "Producers did not finish in time");

        // Wait for the single-writer consumer thread to process everything
        // Since we don't have a direct hook into the consumer's completion, we'll poll the DB count
        long count = 0;
        int attempts = 0;
        while (count < totalRequests && attempts < 100) {
            Thread.sleep(500);
            count = documentRepository.count();
            attempts++;
        }

        assertTrue(count >= totalRequests, "All queued documents should be persisted by the DocumentWriterService");
    }
}
