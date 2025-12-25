package com.agenthub.knowledge.agent;

import com.agenthub.core.agent.Agent;
import com.agenthub.core.agent.AgentContext;
import com.agenthub.core.agent.AgentResult;
import com.agenthub.db.entity.KnowledgeSubscriptionEntity;
import com.agenthub.db.repo.KnowledgeArticleRepository;
import com.agenthub.db.repo.KnowledgeSubscriptionRepository;
import com.agenthub.knowledge.service.KnowledgeIngestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeIngestAgent implements Agent {
    private final KnowledgeSubscriptionRepository subscriptionRepo;
    private final KnowledgeArticleRepository articleRepo;
    private final KnowledgeIngestService ingestService;

    @Override
    public String name() {
        return "knowledge-ingest";
    }

    @Override
    public AgentResult run(AgentContext context) {
        List<KnowledgeSubscriptionEntity> subs = subscriptionRepo.findByEnabledTrueOrderByNameAsc();
        int discovered = 0, saved = 0, skipped = 0, failed = 0;
        int subscriptionSkippedByInterval = 0;

        int minIntervalMinutes = getInt(context, "minIntervalMinutes", 0);
        Instant now = Instant.now();

        for (KnowledgeSubscriptionEntity sub : subs) {
            // 更友好的跳过策略：用该订阅下最新文章的 fetchedAt 判断是否需要再次拉取。
            // 这样本次拉取如果超时/失败，不会因为写入 lastIngestedAt 而阻止后续重试。
            if (minIntervalMinutes > 0) {
                Instant latestFetchedAt = articleRepo.findLatestFetchedAtBySubscriptionId(sub.getId());
                if (latestFetchedAt != null) {
                    long minutes = Duration.between(latestFetchedAt, now).toMinutes();
                if (minutes >= 0 && minutes < minIntervalMinutes) {
                    subscriptionSkippedByInterval++;
                        log.info("按时间间隔跳过订阅：id={}, name={}, latestFetchedAt={}, minutesAgo={}, minIntervalMinutes={}",
                                sub.getId(), sub.getName(), latestFetchedAt, minutes, minIntervalMinutes);
                    continue;
                }
                }
            }

            KnowledgeIngestService.IngestStats s = ingestService.ingestSubscription(sub);
            discovered += s.discovered();
            saved += s.saved();
            skipped += s.skipped();
            failed += s.failed();

            // last_ingested_at 仅作为观测字段：本次订阅拉取没有失败才更新（失败则留空/旧值，方便重试）
            if (s.failed() == 0) {
                sub.setLastIngestedAt(Instant.now());
                subscriptionRepo.save(sub);
            }
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("subscriptions", subs.size());
        stats.put("subscriptionSkippedByInterval", subscriptionSkippedByInterval);
        stats.put("minIntervalMinutes", minIntervalMinutes);
        stats.put("discovered", discovered);
        stats.put("saved", saved);
        stats.put("skipped", skipped);
        stats.put("failed", failed);

        return failed > 0
                ? AgentResult.fail("Completed with failures", stats)
                : AgentResult.ok("Completed", stats);
    }

    private int getInt(AgentContext ctx, String key, int defaultVal) {
        if (ctx == null || ctx.attributes() == null) return defaultVal;
        Object v = ctx.attributes().get(key);
        if (v == null) return defaultVal;
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception ignore) { return defaultVal; }
    }
}


