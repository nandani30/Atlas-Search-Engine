package com.atlas.searchengine.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BM25RankerTest {

    @Test
    void testScoreTerm() {
        BM25Ranker ranker = new BM25Ranker();
        
        // basic sanity check
        double score1 = ranker.scoreTerm(1, 100, 100.0, 10, 100);
        double score2 = ranker.scoreTerm(2, 100, 100.0, 10, 100);
        
        // higher term frequency should give higher score
        assertTrue(score2 > score1);
        
        // Term frequency saturation
        double score3 = ranker.scoreTerm(100, 100, 100.0, 10, 100);
        double score4 = ranker.scoreTerm(1000, 100, 100.0, 10, 100);
        
        // score4 shouldn't be 10x score3, it should be saturated
        assertTrue(score4 > score3);
        assertTrue((score4 / score3) < 2.0); // very saturated
        
        // Length normalization
        double scoreLong = ranker.scoreTerm(1, 200, 100.0, 10, 100);
        // longer document -> lower score for same term freq
        assertTrue(score1 > scoreLong);
    }
}
