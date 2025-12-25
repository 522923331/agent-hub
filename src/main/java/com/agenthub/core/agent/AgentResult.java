package com.agenthub.core.agent;

import java.util.Map;

public record AgentResult(
        boolean success,
        String message,
        Map<String, Object> stats
) {
    public static AgentResult ok(String message, Map<String, Object> stats) {
        return new AgentResult(true, message, stats == null ? Map.of() : stats);
    }

    public static AgentResult fail(String message, Map<String, Object> stats) {
        return new AgentResult(false, message, stats == null ? Map.of() : stats);
    }
}


