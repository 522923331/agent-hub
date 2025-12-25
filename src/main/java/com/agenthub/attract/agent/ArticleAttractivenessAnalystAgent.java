package com.agenthub.attract.agent;

import com.agenthub.attract.model.EngagementSignals;
import com.agenthub.attract.service.AttractivenessScoringService;
import com.agenthub.attract.service.EngagementSignalService;
import com.agenthub.attract.service.EngagementSignalService.ExtractResult;
import com.agenthub.core.agent.Agent;
import com.agenthub.core.agent.AgentContext;
import com.agenthub.core.agent.AgentResult;
import com.agenthub.db.entity.KnowledgeArticleEntity;
import com.agenthub.db.repo.KnowledgeArticleRepository;
import com.agenthub.knowledge.fetch.HttpArticleFetcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleAttractivenessAnalystAgent implements Agent {
    private final KnowledgeArticleRepository articleRepo;
    private final HttpArticleFetcher fetcher;
    private final EngagementSignalService signalService;
    private final AttractivenessScoringService scoringService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String name() {
        return "article-attractiveness-analyst";
    }

    @Override
    public AgentResult run(AgentContext context) {
        int maxArticles = getInt(context, "maxArticles", 80);
        int lookbackDays = getInt(context, "lookbackDays", 7);
        int rescoreAfterHours = getInt(context, "rescoreAfterHours", 24);

        Instant agentStartAt = Instant.now();
        Instant since = Instant.now().minus(lookbackDays, ChronoUnit.DAYS);
        Instant rescoreBefore = Instant.now().minus(rescoreAfterHours, ChronoUnit.HOURS);

        log.info("文章吸引力分析Agent开始：maxArticles={}, lookbackDays={}, rescoreAfterHours={}, since={}, rescoreBefore={}",
                maxArticles, lookbackDays, rescoreAfterHours, since, rescoreBefore);

        Page<KnowledgeArticleEntity> page = articleRepo.findCandidatesForScoring(
                since,
                rescoreBefore,
                PageRequest.of(0, Math.max(1, Math.min(maxArticles, 200)), Sort.by(Sort.Direction.DESC, "fetchedAt"))
        );

        int scored = 0;
        int failed = 0;
        int total = page.getNumberOfElements();
        Map<String, Integer> sourceCounter = new HashMap<>();
        sourceCounter.put("none", 0);
        long batchStartMs = System.currentTimeMillis();

        log.info("文章吸引力分析候选集：本次候选数={}, pageSize={}, 总候选数(本次)={}, 排序=fetchedAt DESC",
                total, page.getSize(), total);

        for (KnowledgeArticleEntity a : page.getContent()) {
            long oneStartMs = System.currentTimeMillis();
            try {
                log.info("开始处理文章：id={}, url={}, publishedAt={}, fetchedAt={}, 当前score={}, 当前scoredAt={}",
                        a.getId(), a.getUrl(), a.getPublishedAt(), a.getFetchedAt(), a.getAttractivenessScore(), a.getScoredAt());

                // 抓取 HTML 以抽取热度信号（只用于计算，不入库保存原文）
                String html = fetcher.fetchHtml(a.getUrl());
                if (html == null || html.isBlank()) {
                    throw new IllegalStateException("HTML为空");
                }

                ExtractResult r = signalService.extractWithSource(a.getUrl(), html);
                String extractor = r.extractor() == null ? "none" : r.extractor();
                EngagementSignals sig = r.signals() == null ? EngagementSignals.empty() : r.signals();
                sourceCounter.put(extractor, sourceCounter.getOrDefault(extractor, 0) + 1);

                log.info("热度信号抽取完成：id={}, extractor={}, view={}, comment={}, like={}, share={}",
                        a.getId(), extractor, sig.viewCount(), sig.commentCount(), sig.likeCount(), sig.shareCount());

                a.setViewCount(sig.viewCount());
                a.setCommentCount(sig.commentCount());
                a.setLikeCount(sig.likeCount());
                a.setShareCount(sig.shareCount());

                BigDecimal score = scoringService.score(sig, a.getPublishedAt());
                a.setAttractivenessScore(score);
                a.setScoredAt(Instant.now());
                a.setAttractivenessSignalsJson(writeJson(sig));
                articleRepo.save(a);
                scored++;

                log.info("文章吸引力评分完成：id={}, score={}, 耗时={}ms",
                        a.getId(), score, (System.currentTimeMillis() - oneStartMs));
            } catch (Exception e) {
                failed++;
                log.warn("文章吸引力评分失败：id={}, url={}, err={}", a.getId(), a.getUrl(), e.toString());
                log.debug("文章吸引力评分失败堆栈：id={}", a.getId(), e);
            }

            // 每处理 10 条输出一次阶段性进度，便于观察卡点
            int processed = scored + failed;
            if (processed % 10 == 0 || processed == total) {
                long elapsedMs = System.currentTimeMillis() - batchStartMs;
                log.info("文章吸引力分析进度：processed={}/{}, scored={}, failed={}, 阶段耗时={}ms",
                        processed, total, scored, failed, elapsedMs);
                batchStartMs = System.currentTimeMillis();
            }
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("candidates", page.getNumberOfElements());
        stats.put("scored", scored);
        stats.put("failed", failed);
        stats.put("lookbackDays", lookbackDays);
        stats.put("maxArticles", maxArticles);
        stats.put("rescoreAfterHours", rescoreAfterHours);
        stats.put("signalSources", sourceCounter);
        stats.put("elapsedMs", Duration.between(agentStartAt, Instant.now()).toMillis());

        log.info("文章吸引力分析Agent结束：candidates={}, scored={}, failed={}, signalSources={}, 总耗时={}ms",
                total, scored, failed, sourceCounter, Duration.between(agentStartAt, Instant.now()).toMillis());

        return failed > 0 ? AgentResult.fail("完成但有失败", stats) : AgentResult.ok("完成", stats);
    }

    private String writeJson(EngagementSignals s) {
        try { return objectMapper.writeValueAsString(s); } catch (Exception e) { return null; }
    }

    private int getInt(AgentContext ctx, String key, int defaultVal) {
        if (ctx == null || ctx.attributes() == null) return defaultVal;
        Object v = ctx.attributes().get(key);
        if (v == null) return defaultVal;
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception ignore) { return defaultVal; }
    }
}


