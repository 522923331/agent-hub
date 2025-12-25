package com.agenthub.knowledge.model;

import java.time.Instant;

public record ExtractedArticle(
        DiscoveredArticle discovered,
        Instant fetchedAt,
        String rawHtml,
        String extractedText,
        String detectedLang
) {
}


