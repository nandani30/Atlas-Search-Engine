package com.atlas.searchengine.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class SearchAnalyticsService {

    // query -> count
    private final Map<String, AtomicInteger> allQueries = new ConcurrentHashMap<>();
    // collection -> query -> count
    private final Map<String, Map<String, AtomicInteger>> queriesByCollection = new ConcurrentHashMap<>();

    public void recordQuery(String query, String collection) {
        if (query == null || query.trim().isEmpty()) return;
        
        String normalizedQuery = query.trim().toLowerCase();

        allQueries.computeIfAbsent(normalizedQuery, k -> new AtomicInteger(0)).incrementAndGet();

        if (collection != null && !collection.isEmpty() && !"all".equalsIgnoreCase(collection)) {
            queriesByCollection.computeIfAbsent(collection, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(normalizedQuery, k -> new AtomicInteger(0))
                    .incrementAndGet();
        }
    }

    public List<String> getTrendingQueries(String collection, int limit) {
        Map<String, AtomicInteger> targetMap;
        
        if ("all".equalsIgnoreCase(collection) || collection == null || collection.isEmpty()) {
            targetMap = allQueries;
        } else {
            targetMap = queriesByCollection.getOrDefault(collection, new ConcurrentHashMap<>());
        }

        return targetMap.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue().get(), e1.getValue().get()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
