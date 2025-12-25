package com.agenthub.llm;

import com.agenthub.knowledge.model.EnrichedArticle;
import com.agenthub.knowledge.model.ExtractedArticle;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.llm", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpLanguageModelClient implements LanguageModelClient {
    @Override
    public EnrichedArticle enrich(ExtractedArticle article) {
        String title = article.discovered() == null ? "" : article.discovered().title();
        String text = article.extractedText() == null ? "" : article.extractedText();
        return new EnrichedArticle(
                title,
                text,
                "（未配置 LLM：暂未生成读后感。你可以开启 app.llm.enabled=true 并配置 app.llm.base-url/api-key/model）"
        );
    }
}


