package com.atlas.searchengine.model;

import java.util.List;

public record SearchResponse(
    List<SearchResult> results,
    List<String> didYouMeanSuggestions,
    String message,
    long totalResults,
    int totalPages
) {}
