package com.atlas.searchengine.controller;

import com.atlas.searchengine.model.PagedResult;
import com.atlas.searchengine.model.SearchResponse;
import com.atlas.searchengine.model.SearchResult;
import com.atlas.searchengine.model.Document;
import com.atlas.searchengine.model.DocumentRepository;
import com.atlas.searchengine.service.IndexerService;
import com.atlas.searchengine.service.SearchAnalyticsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api")
public class SearchController {

    private final IndexerService indexerService;
    private final SearchAnalyticsService searchAnalyticsService;
    private final DocumentRepository documentRepository;

    public SearchController(IndexerService indexerService, SearchAnalyticsService searchAnalyticsService, DocumentRepository documentRepository) {
        this.indexerService = indexerService;
        this.searchAnalyticsService = searchAnalyticsService;
        this.documentRepository = documentRepository;
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
        long totalElements = pagedResult.totalElements();
        
        // Deep Database Hybrid Search: If RAM index yields very few results, fallback to the 800,000 document Turso archive
        // We only trigger this for queries 3 characters or longer to prevent massive slow full-table scans for 'a' or '.'
        if (totalElements < 10 && q.trim().length() >= 3) {
            Page<Document> dbFallbackPage = documentRepository.searchDatabaseFallback("%" + q + "%", PageRequest.of(page, pageSize));
            if (dbFallbackPage.hasContent()) {
                List<String> queryTokens = List.of(q.split(" "));
                List<SearchResult> fallbackResults = dbFallbackPage.getContent().stream()
                        .map(doc -> new SearchResult(
                                doc.getId(), 
                                doc.getSourceUrl(), 
                                doc.getTitle(), 
                                indexerService.generateSnippet(doc.getText(), queryTokens), 
                                0.1, // Fixed low score for fallback results
                                doc.getCollection()
                        )).toList();
                
                // Return fallback results directly
                return ResponseEntity.ok(new SearchResponse(fallbackResults, Collections.emptyList(), "Deep Database Fallback", dbFallbackPage.getTotalElements(), dbFallbackPage.getTotalPages()));
            }
        }
        
        if (results.isEmpty()) {
            List<String> didYouMean = indexerService.getDidYouMean(q, collection);
            return ResponseEntity.ok(new SearchResponse(Collections.emptyList(), didYouMean, "no results", 0, 0));
        }
        
        return ResponseEntity.ok(new SearchResponse(results, Collections.emptyList(), "success", totalElements, pagedResult.totalPages()));
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

    @GetMapping("/latest")
    public ResponseEntity<List<SearchResult>> latest(
            @RequestParam(defaultValue = "all") String collection,
            @RequestParam(defaultValue = "5") int limit) {
        
        List<SearchResult> latest = indexerService.getLatestPublishedDocuments(collection, limit);
        return ResponseEntity.ok(latest);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("ok");
    }
}
