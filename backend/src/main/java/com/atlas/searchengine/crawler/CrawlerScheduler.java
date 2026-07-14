package com.atlas.searchengine.crawler;

import com.atlas.searchengine.service.IndexerService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CrawlerScheduler {

    private final LearningCrawler learningCrawler;
    private final ScienceCrawler scienceCrawler;
    private final NewsCrawler newsCrawler;
    private final IndexerService indexerService;
    private final com.atlas.searchengine.model.DocumentRepository documentRepository;

    public CrawlerScheduler(LearningCrawler learningCrawler, ScienceCrawler scienceCrawler, NewsCrawler newsCrawler, IndexerService indexerService, com.atlas.searchengine.model.DocumentRepository documentRepository) {
        this.learningCrawler = learningCrawler;
        this.scienceCrawler = scienceCrawler;
        this.newsCrawler = newsCrawler;
        this.indexerService = indexerService;
        this.documentRepository = documentRepository;
    }

    // Run every 1 hour (3600000 ms), but wait 60 seconds after startup
    @Scheduled(initialDelay = 60000, fixedDelay = 3600000)
    public void runCrawlers() {
        System.out.println("Starting scheduled re-crawling...");

        Thread t1 = new Thread(() -> {
            learningCrawler.crawlBatch(50);
            indexerService.rebuildCollection("learning", documentRepository.findByCollection("learning"));
        });

        Thread t2 = new Thread(() -> {
            newsCrawler.crawlBatch(50);
            indexerService.rebuildCollection("news", documentRepository.findByCollection("news"));
        });

        Thread t3 = new Thread(() -> {
            scienceCrawler.crawlBatch(50);
            indexerService.rebuildCollection("science", documentRepository.findByCollection("science"));
        });

        t1.start();
        t2.start();
        t3.start();
    }
}
