package com.agenthub.knowledge.service;

import com.agenthub.db.entity.KnowledgeArticleEntity;
import com.agenthub.db.entity.KnowledgeSubscriptionEntity;
import com.agenthub.db.repo.KnowledgeArticleRepository;
import com.agenthub.knowledge.extract.ArticleExtractor;
import com.agenthub.knowledge.extract.LanguageDetector;
import com.agenthub.knowledge.fetch.HttpArticleFetcher;
import com.agenthub.knowledge.model.DiscoveredArticle;
import com.agenthub.knowledge.model.EnrichedArticle;
import com.agenthub.knowledge.model.ExtractedArticle;
import com.agenthub.knowledge.source.KnowledgeSource;
import com.agenthub.knowledge.source.KnowledgeSourceRegistry;
import com.agenthub.llm.LanguageModelClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeIngestService {
    private final KnowledgeSourceRegistry sourceRegistry;
    private final HttpArticleFetcher fetcher;
    private final ArticleExtractor extractor;
    private final LanguageDetector languageDetector;
    private final LanguageModelClient llm;
    private final KnowledgeArticleRepository articleRepo;

    public IngestStats ingestSubscription(KnowledgeSubscriptionEntity sub) {
        KnowledgeSource source = sourceRegistry.findByType(sub.getSourceType())
                .orElseThrow(() -> new IllegalArgumentException("Unsupported sourceType: " + sub.getSourceType()));

        List<DiscoveredArticle> discovered = safeList(source.discover(sub));
        int discoveredCount = discovered.size();
        int saved = 0;
        int skipped = 0;
        int failed = 0;

        for (DiscoveredArticle d : discovered) {
            if (d.url() == null || d.url().isBlank()) continue;
            if (articleRepo.existsByUrl(d.url())) {
                skipped++;
                continue;
            }

            KnowledgeArticleEntity entity = new KnowledgeArticleEntity();
            entity.setId(UUID.randomUUID());
            entity.setSubscriptionId(sub.getId());
            entity.setSourceName(sub.getName());
            entity.setUrl(d.url());
            entity.setTitle(d.title());
            entity.setPublishedAt(d.publishedAt());
            entity.setStatus("NEEDS_REVIEW");

            try {
                Instant fetchedAt = Instant.now();
                String html = fetcher.fetchHtml(d.url());
                String extractedText = extractor.extractText(html, d.url());
                String lang = languageDetector.detect(extractedText);

                entity.setFetchedAt(fetchedAt);
                entity.setRawHtml(truncate(html, 2_000_000));
                entity.setExtractedText(truncate(extractedText, 200_000));
                entity.setDetectedLang(lang);

                ExtractedArticle extracted = new ExtractedArticle(d, fetchedAt, entity.getRawHtml(), entity.getExtractedText(), lang);
                EnrichedArticle enriched = llm.enrich(extracted);

                entity.setZhTitle(truncate(enriched.zhTitle(), 6000));
                entity.setZhContent(truncate(enriched.zhContent(), 2_000_000));
                entity.setReflection(truncate(enriched.reflection(), 50_000));

                boolean llmSeemsEnabled = enriched.reflection() != null && !enriched.reflection().contains("未配置 LLM");
                entity.setStatus(llmSeemsEnabled ? "DONE" : "NEEDS_REVIEW");
                articleRepo.save(entity);
                saved++;
            } catch (Exception e) {
                entity.setStatus("ERROR");
                entity.setErrorMessage(truncate(e.getMessage(), 10_000));
                articleRepo.save(entity);
                failed++;
                log.warn("Ingest failed: sub={}, url={}, err={}", sub.getName(), d.url(), e.toString());
            }
        }

        return new IngestStats(discoveredCount, saved, skipped, failed);
    }

    private List<DiscoveredArticle> safeList(List<DiscoveredArticle> v) {
        return v == null ? new ArrayList<>() : v;
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        return s.substring(0, max);
    }

    public record IngestStats(int discovered, int saved, int skipped, int failed) {}
}


