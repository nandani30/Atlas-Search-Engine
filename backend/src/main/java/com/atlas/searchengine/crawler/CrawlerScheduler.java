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

    @Scheduled(fixedDelay = 43200000) // Every 12 hours
    public void scheduleLearningCrawler() {
        if (learningCrawler != null) {
            learningCrawler.crawlBatch(20);
            indexerService.rebuildCollectionFromDB("learning", documentRepository);
        }
    }

    @Scheduled(fixedDelay = 43200000)
    public void scheduleNewsCrawler() {
        if (newsCrawler != null) {
            newsCrawler.crawlBatch(20);
            indexerService.rebuildCollectionFromDB("news", documentRepository);
        }
    }

    @Scheduled(fixedDelay = 43200000)
    public void scheduleScienceCrawler() {
        if (scienceCrawler != null) {
            scienceCrawler.crawlBatch(20);
            indexerService.rebuildCollectionFromDB("science", documentRepository);
        }
    }
}
