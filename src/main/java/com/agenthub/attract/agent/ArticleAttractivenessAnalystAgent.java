package com.agenthub.attract.agent;

import com.agenthub.attract.model.EngagementSignals;
import com.agenthub.attract.service.AttractivenessScoringService;
import com.agenthub.attract.service.EngagementSignalService;
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

import java.time.Instant;
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

        Instant since = Instant.now().minus(lookbackDays, ChronoUnit.DAYS);
        Instant rescoreBefore = Instant.now().minus(rescoreAfterHours, ChronoUnit.HOURS);

        Page<KnowledgeArticleEntity> page = articleRepo.findCandidatesForScoring(
                since,
                rescoreBefore,
                PageRequest.of(0, Math.max(1, Math.min(maxArticles, 200)), Sort.by(Sort.Direction.DESC, "fetchedAt"))
        );

        int scored = 0;
        int failed = 0;
        for (KnowledgeArticleEntity a : page.getContent()) {
            try {
                // 抓取 HTML 以抽取热度信号（只用于计算，不入库保存原文）
                String html = fetcher.fetchHtml(a.getUrl());
                EngagementSignals sig = signalService.extract(a.getUrl(), html);
                a.setViewCount(sig.viewCount());
                a.setCommentCount(sig.commentCount());
                a.setLikeCount(sig.likeCount());
                a.setShareCount(sig.shareCount());

                a.setAttractivenessScore(scoringService.score(sig, a.getPublishedAt()));
                a.setScoredAt(Instant.now());
                a.setAttractivenessSignalsJson(writeJson(sig));
                articleRepo.save(a);
                scored++;
            } catch (Exception e) {
                failed++;
                log.warn("吸引力评分计算失败：id={}, url={}, err={}", a.getId(), a.getUrl(), e.toString());
            }
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("candidates", page.getNumberOfElements());
        stats.put("scored", scored);
        stats.put("failed", failed);
        stats.put("lookbackDays", lookbackDays);
        stats.put("maxArticles", maxArticles);

        return failed > 0 ? AgentResult.fail("Completed with failures", stats) : AgentResult.ok("Completed", stats);
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


