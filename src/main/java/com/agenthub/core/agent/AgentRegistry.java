package com.agenthub.core.agent;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AgentRegistry {
    private final Map<String, Agent> agentsByName;

    public AgentRegistry(List<Agent> agents) {
        this.agentsByName = agents.stream()
                .collect(Collectors.toMap(Agent::name, Function.identity(), (a, b) -> a));
    }

    public List<String> listAgentNames() {
        return agentsByName.keySet().stream().sorted(Comparator.naturalOrder()).toList();
    }

    public Optional<Agent> findByName(String name) {
        if (name == null) return Optional.empty();
        return Optional.ofNullable(agentsByName.get(name));
    }
}


