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
import com.agenthub.util.LarkAlarmUtil;
import com.agenthub.util.Sha256;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
    private final LarkAlarmUtil larkAlarmUtil;
    private final Environment environment;

    public IngestStats ingestSubscription(KnowledgeSubscriptionEntity sub) {
        long start = System.currentTimeMillis();
        log.info("订阅拉取开始：id={}, name={}, sourceType={}, enabled={}, limit={}",
                sub.getId(), sub.getName(), sub.getSourceType(), sub.isEnabled(), sub.getFetchLimit());
        KnowledgeSource source = sourceRegistry.findByType(sub.getSourceType())
                .orElseThrow(() -> new IllegalArgumentException("Unsupported sourceType: " + sub.getSourceType()));

        List<DiscoveredArticle> discovered = safeList(source.discover(sub));
        int discoveredCount = discovered.size();
        int saved = 0;
        int skipped = 0;
        int failed = 0;
        int targetNewLimit = Math.max(1, sub.getFetchLimit());

        for (DiscoveredArticle d : discovered) {
            if (saved >= targetNewLimit) break;
            if (d.url() == null || d.url().isBlank()) continue;
            String urlHash = Sha256.hex(d.url());
            if (articleRepo.existsByUrlHash(urlHash)) {
                skipped++;
                continue;
            }

            KnowledgeArticleEntity entity = new KnowledgeArticleEntity();
            entity.setSubscriptionId(sub.getId());
            entity.setSourceName(sub.getName());
            entity.setUrl(d.url());
            entity.setUrlHash(urlHash);
            entity.setTitle(d.title());
            entity.setPublishedAt(d.publishedAt());
            entity.setStatus("NEEDS_REVIEW");

            try {
                Instant fetchedAt = Instant.now();
                String html = fetcher.fetchHtml(d.url());
                String extractedText = extractor.extractText(html, d.url());
                String lang = languageDetector.detect(extractedText);

                entity.setFetchedAt(fetchedAt);
                entity.setExtractedText(truncate(extractedText, 200_000));
                entity.setDetectedLang(lang);

                ExtractedArticle extracted = new ExtractedArticle(d, fetchedAt, entity.getExtractedText(), lang, sub.getTags());
                EnrichedArticle enriched = llm.enrich(extracted);

                entity.setZhTitle(truncate(enriched.zhTitle(), 6000));
                entity.setZhContent(truncate(enriched.zhContent(), 2_000_000));
                entity.setReflection(truncate(enriched.reflection(), 50_000));

                boolean llmSeemsEnabled = enriched.reflection() != null && !enriched.reflection().contains("未配置 LLM");
                entity.setStatus(llmSeemsEnabled ? "DONE" : "NEEDS_REVIEW");
                articleRepo.save(entity);
                saved++;

                // 新文章生成后通知飞书（不包含正文/敏感信息）
                try {
                    String title = entity.getZhTitle();
                    if (title == null || title.isBlank()) title = entity.getTitle();
                    title = truncate(title, 200);
                    String articlePageUrl = buildArticlePageUrl(entity.getId());
                    String msg = "新文章已生成\n"
                            + "订阅: " + sub.getZhName() + " (" + sub.getName() + ")\n"
                            + "状态: " + entity.getStatus() + "\n"
                            + "标题: " + (title == null ? "" : title) + "\n"
                            + "链接: " + articlePageUrl;
                    larkAlarmUtil.sendTextAlarm(msg);
                } catch (Exception notifyEx) {
                    log.warn("飞书通知失败：subId={}, urlHash={}, err={}", sub.getId(), urlHash, notifyEx.toString());
                }
            } catch (Exception e) {
                entity.setStatus("ERROR");
                entity.setErrorMessage(truncate(e.getMessage(), 10_000));
                articleRepo.save(entity);
                failed++;
                log.warn("文章拉取/解读失败：subId={}, subName={}, urlHash={}, url={}, err={}",
                        sub.getId(), sub.getName(), urlHash, d.url(), e.toString());
            }
        }

        log.info("订阅拉取结束：id={}, name={}, discovered={}, saved={}, skipped={}, failed={}, tookMs={}",
                sub.getId(), sub.getName(), discoveredCount, saved, skipped, failed, (System.currentTimeMillis() - start));
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

    /**
     * 本站文章详情链接（用于飞书通知）。
     * - 支持配置 app.public-base-url（如 https://example.com），否则返回相对路径（/articles.html?id=123）。
     */
    private String buildArticlePageUrl(Long articleId) {
        String path = "/articles.html?id=" + (articleId == null ? "" : articleId);
        String base = environment.getProperty("app.public-base-url", "");
        if (base == null) base = "";
        base = base.trim();
        if (base.isBlank()) return path;
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base + path;
    }

    public record IngestStats(int discovered, int saved, int skipped, int failed) {}
}


