package com.atlas.searchengine.model;

import java.time.LocalDateTime;

public record DocumentWriteRequest(
        String id,
        String title,
        String text,
        String collection,
        String sourceUrl,
        LocalDateTime crawledAt,
        LocalDateTime publishedAt
) {}
