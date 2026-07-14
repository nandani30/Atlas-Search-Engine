package com.atlas.searchengine.model;

public record SearchResult(
    String id,
    String url,
    String title,
    String snippet,
    double score,
    String sourceCollection
) {}
