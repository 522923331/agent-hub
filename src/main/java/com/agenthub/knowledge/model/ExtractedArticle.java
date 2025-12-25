package com.agenthub.knowledge.model;

import java.time.Instant;

public record ExtractedArticle(
        DiscoveredArticle discovered,
        Instant fetchedAt,
        String extractedText,
        String detectedLang
) {
}


