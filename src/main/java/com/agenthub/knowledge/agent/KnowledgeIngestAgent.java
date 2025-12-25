package com.agenthub.knowledge.agent;

import com.agenthub.core.agent.Agent;
import com.agenthub.core.agent.AgentContext;
import com.agenthub.core.agent.AgentResult;
import com.agenthub.db.entity.KnowledgeSubscriptionEntity;
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
            if (minIntervalMinutes > 0 && sub.getLastIngestedAt() != null) {
                long minutes = Duration.between(sub.getLastIngestedAt(), now).toMinutes();
                if (minutes >= 0 && minutes < minIntervalMinutes) {
                    subscriptionSkippedByInterval++;
                    log.info("按时间间隔跳过订阅：id={}, name={}, lastIngestedAt={}, minutesAgo={}, minIntervalMinutes={}",
                            sub.getId(), sub.getName(), sub.getLastIngestedAt(), minutes, minIntervalMinutes);
                    continue;
                }
            }

            KnowledgeIngestService.IngestStats s = ingestService.ingestSubscription(sub);
            discovered += s.discovered();
            saved += s.saved();
            skipped += s.skipped();
            failed += s.failed();

            // 标记已拉取（无论是否拉到新文章，避免重复频繁拉取）
            sub.setLastIngestedAt(now);
            subscriptionRepo.save(sub);
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


