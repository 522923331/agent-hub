package com.agenthub.core.agent;

import com.agenthub.db.entity.AgentRunEntity;
import com.agenthub.db.repo.AgentRunRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentRunnerService {
    private final AgentRegistry agentRegistry;
    private final AgentRunRepository agentRunRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AgentResult runByName(String agentName) {
        Agent agent = agentRegistry.findByName(agentName)
                .orElseThrow(() -> new IllegalArgumentException("Unknown agent: " + agentName));

        AgentRunEntity run = new AgentRunEntity();
        run.setId(UUID.randomUUID());
        run.setAgentName(agent.name());
        run.setStartedAt(Instant.now());
        run.setStatus("RUNNING");
        agentRunRepository.save(run);

        try {
            AgentResult result = agent.run(AgentContext.empty());
            run.setFinishedAt(Instant.now());
            run.setStatus(result.success() ? "SUCCESS" : "FAILED");
            run.setStatsJson(writeJson(result.stats()));
            run.setErrorMessage(result.success() ? null : result.message());
            agentRunRepository.save(run);
            return result;
        } catch (Exception e) {
            log.error("Agent run failed: agent={}", agent.name(), e);
            run.setFinishedAt(Instant.now());
            run.setStatus("FAILED");
            run.setErrorMessage(e.getMessage());
            agentRunRepository.save(run);
            return AgentResult.fail("Agent failed: " + e.getMessage(), Map.of());
        }
    }

    private String writeJson(Map<String, Object> stats) {
        try {
            if (stats == null) return null;
            return objectMapper.writeValueAsString(stats);
        } catch (Exception e) {
            return null;
        }
    }
}


