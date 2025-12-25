package com.agenthub.knowledge.agent;

import com.agenthub.core.agent.Agent;
import com.agenthub.core.agent.AgentContext;
import com.agenthub.core.agent.AgentResult;
import com.agenthub.db.entity.KnowledgeSubscriptionEntity;
import com.agenthub.db.repo.KnowledgeSubscriptionRepository;
import com.agenthub.knowledge.service.KnowledgeIngestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        for (KnowledgeSubscriptionEntity sub : subs) {
            KnowledgeIngestService.IngestStats s = ingestService.ingestSubscription(sub);
            discovered += s.discovered();
            saved += s.saved();
            skipped += s.skipped();
            failed += s.failed();
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("subscriptions", subs.size());
        stats.put("discovered", discovered);
        stats.put("saved", saved);
        stats.put("skipped", skipped);
        stats.put("failed", failed);

        return failed > 0
                ? AgentResult.fail("Completed with failures", stats)
                : AgentResult.ok("Completed", stats);
    }
}


