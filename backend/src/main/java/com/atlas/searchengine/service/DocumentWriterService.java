package com.atlas.searchengine.service;

import com.atlas.searchengine.model.Document;
import com.atlas.searchengine.model.DocumentRepository;
import com.atlas.searchengine.model.DocumentWriteRequest;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class DocumentWriterService {

    private final DocumentWriteQueue writeQueue;
    private final DocumentRepository documentRepository;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public DocumentWriterService(DocumentWriteQueue writeQueue, DocumentRepository documentRepository) {
        this.writeQueue = writeQueue;
        this.documentRepository = documentRepository;
    }

    @PostConstruct
    public void startWriterThread() {
        executorService.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    DocumentWriteRequest req = writeQueue.dequeue();
                    // Turso's SQLite-based engine does not yet fully support concurrent writers,
                    // so all writes are serialized through a single writer thread 
                    // to avoid SQLite "database is locked" write conflicts between the three independently-scheduled crawlers.
                    
                    Document doc = new Document(
                            req.id(),
                            req.title(),
                            req.text(),
                            req.collection(),
                            req.sourceUrl(),
                            req.crawledAt()
                    );
                    
                    documentRepository.save(doc);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("Error saving document to DB: " + e.getMessage());
                }
            }
        });
    }

    @PreDestroy
    public void stopWriterThread() {
        executorService.shutdownNow();
    }
}
