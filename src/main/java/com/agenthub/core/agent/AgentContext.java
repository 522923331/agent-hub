package com.agenthub.core.agent;

import java.time.Instant;
import java.util.Map;

public record AgentContext(
        Instant startedAt,
        Map<String, Object> attributes
){
    public static AgentContext empty() {
        return new AgentContext(Instant.now(), Map.of());
    }
}


