package com.atlas.searchengine.model;

import java.util.List;

public record PagedResult<T>(
    List<T> content,
    long totalElements,
    int totalPages
) {}
