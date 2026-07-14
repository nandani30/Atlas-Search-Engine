package com.atlas.searchengine.service;

import com.atlas.searchengine.model.Document;
import com.atlas.searchengine.model.DocumentRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StartupIndexer implements ApplicationRunner {

    private final DocumentRepository documentRepository;
    private final IndexerService indexerService;

    public StartupIndexer(DocumentRepository documentRepository, IndexerService indexerService) {
        this.documentRepository = documentRepository;
        this.indexerService = indexerService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("Indexing Documents from Database...");
        String[] collections = {"learning", "science", "news"};
        for (String col : collections) {
            indexerService.rebuildCollectionFromDB(col, documentRepository);
        }
        System.out.println("Indexing Complete.");
    }
}
