package com.atlas.searchengine.controller;

import com.atlas.searchengine.model.PagedResult;
import com.atlas.searchengine.model.SearchResponse;
import com.atlas.searchengine.model.SearchResult;
import com.atlas.searchengine.service.IndexerService;
import com.atlas.searchengine.service.SearchAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api")
public class SearchController {

    private final IndexerService indexerService;
    private final SearchAnalyticsService searchAnalyticsService;

    public SearchController(IndexerService indexerService, SearchAnalyticsService searchAnalyticsService) {
        this.indexerService = indexerService;
        this.searchAnalyticsService = searchAnalyticsService;
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "all") String collection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        
        searchAnalyticsService.recordQuery(q, collection);
        
        PagedResult<SearchResult> pagedResult = indexerService.search(q, collection, page, pageSize);
        List<SearchResult> results = pagedResult.content();
        
        if (results.isEmpty()) {
            List<String> didYouMean = indexerService.getDidYouMean(q, collection);
            return ResponseEntity.ok(new SearchResponse(Collections.emptyList(), didYouMean, "no results", 0, 0));
        }
        
        return ResponseEntity.ok(new SearchResponse(results, Collections.emptyList(), "success", pagedResult.totalElements(), pagedResult.totalPages()));
    }

    @GetMapping("/trending")
    public ResponseEntity<List<String>> trending(
            @RequestParam(defaultValue = "all") String collection,
            @RequestParam(defaultValue = "10") int limit) {
        
        List<String> trending = searchAnalyticsService.getTrendingQueries(collection, limit);
        return ResponseEntity.ok(trending);
    }

    @GetMapping("/autocomplete")
    public ResponseEntity<List<String>> autocomplete(
            @RequestParam String prefix,
            @RequestParam(defaultValue = "all") String collection,
            @RequestParam(defaultValue = "5") int limit) {
        
        List<String> suggestions = indexerService.getAutocompleteSuggestions(prefix, collection, limit);
        return ResponseEntity.ok(suggestions);
    }

    @GetMapping("/random-suggestions")
    public ResponseEntity<List<SearchResult>> randomSuggestions(
            @RequestParam(defaultValue = "all") String collection,
            @RequestParam(defaultValue = "5") int count) {
        
        List<SearchResult> results = indexerService.getRandomSuggestions(collection, count);
        return ResponseEntity.ok(results);
    }
}
