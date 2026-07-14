package com.atlas.searchengine.service;

import com.atlas.searchengine.model.DocumentWriteRequest;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class DocumentWriteQueue {

    private final BlockingQueue<DocumentWriteRequest> queue = new LinkedBlockingQueue<>();

    public void enqueue(DocumentWriteRequest request) {
        queue.offer(request);
    }

    public DocumentWriteRequest dequeue() throws InterruptedException {
        return queue.take();
    }
}
