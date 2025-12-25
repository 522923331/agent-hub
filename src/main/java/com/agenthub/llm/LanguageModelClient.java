package com.agenthub.llm;

import com.agenthub.knowledge.model.EnrichedArticle;
import com.agenthub.knowledge.model.ExtractedArticle;

public interface LanguageModelClient {
    EnrichedArticle enrich(ExtractedArticle article);
}


