package com.agenthub.core.agent;

public interface Agent {
    String name();

    AgentResult run(AgentContext context);
}


