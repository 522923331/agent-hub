package com.agenthub.knowledge.model;

import java.time.Instant;

public record ExtractedArticle(
        DiscoveredArticle discovered,
        Instant fetchedAt,
        String extractedText,
        String detectedLang,
        /**
         * 订阅侧的标签/领域信息（可用于细分领域 Prompt 路由）。
         * 约定：通常为逗号分隔，如：emotion,health；也允许中文/任意文本，路由时做关键字匹配。
         */
        String subscriptionTags
) {
}


