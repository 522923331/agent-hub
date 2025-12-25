package com.agenthub.knowledge.model;

import java.time.Instant;

public record DiscoveredArticle(
        String sourceName,
        String title,
        String url,
        Instant publishedAt,
        String summary
) {
}


