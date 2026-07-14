package com.atlas.searchengine.service;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

class IndexerServiceTest {

    @Test
    void testTokenizeAndStopWords() {
        IndexerService indexer = new IndexerService(new BM25Ranker(), new ConcurrentMapCacheManager());
        List<String> tokens = indexer.tokenize("The quick brown fox jumps over the lazy dog!");
        
        // "the" is a stop word, punctuation should be removed
        assertEquals(List.of("quick", "brown", "fox", "jumps", "over", "lazy", "dog"), tokens);
    }
}
