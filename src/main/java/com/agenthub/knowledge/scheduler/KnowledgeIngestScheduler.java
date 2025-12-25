package com.agenthub.knowledge.scheduler;

import com.agenthub.core.agent.AgentRunnerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeIngestScheduler {
    private final AgentRunnerService agentRunnerService;

    @Value("${app.scheduler.knowledge-ingest.enabled:false}")
    private boolean enabled;

    @Scheduled(fixedDelayString = "${app.scheduler.knowledge-ingest.fixed-delay-ms:21600000}", initialDelayString = "30000")
    public void tick() {
        if (!enabled) return;
        try {
            agentRunnerService.runByName("knowledge-ingest");
        } catch (Exception e) {
            log.error("knowledge-ingest scheduled run failed", e);
        }
    }
}


