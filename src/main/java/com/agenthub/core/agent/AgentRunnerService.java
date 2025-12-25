package com.agenthub.core.agent;

import com.agenthub.db.entity.AgentRunEntity;
import com.agenthub.db.repo.AgentRunRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentRunnerService {
    private final AgentRegistry agentRegistry;
    private final AgentRunRepository agentRunRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AgentResult runByName(String agentName) {
        return runByName(agentName, Map.of());
    }

    public AgentResult runByName(String agentName, Map<String, Object> attributes) {
        Agent agent = agentRegistry.findByName(agentName)
                .orElseThrow(() -> new IllegalArgumentException("Unknown agent: " + agentName));

        AgentRunEntity run = new AgentRunEntity();
        run.setId(null);
        run.setAgentName(agent.name());
        run.setStartedAt(Instant.now());
        run.setStatus("RUNNING");
        agentRunRepository.save(run);
        long start = System.currentTimeMillis();
        log.info("Agent 开始运行：id={}, agent={}", run.getId(), agent.name());

        try {
            AgentResult result = agent.run(new AgentContext(Instant.now(), attributes == null ? Map.of() : attributes));
            run.setFinishedAt(Instant.now());
            run.setStatus(result.success() ? "SUCCESS" : "FAILED");
            run.setStatsJson(writeJson(result.stats()));
            run.setErrorMessage(result.success() ? null : result.message());
            agentRunRepository.save(run);
            log.info("Agent 运行结束：id={}, agent={}, status={}, tookMs={}, stats={}",
                    run.getId(), agent.name(), run.getStatus(), (System.currentTimeMillis() - start), result.stats());
            return result;
        } catch (Exception e) {
            log.error("Agent 运行失败：agent={}", agent.name(), e);
            run.setFinishedAt(Instant.now());
            run.setStatus("FAILED");
            run.setErrorMessage(e.getMessage());
            agentRunRepository.save(run);
            log.info("Agent 运行结束：id={}, agent={}, status={}, tookMs={}",
                    run.getId(), agent.name(), run.getStatus(), (System.currentTimeMillis() - start));
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


