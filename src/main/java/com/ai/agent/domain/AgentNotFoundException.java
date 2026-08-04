package com.ai.agent.domain;

import com.ai.agent.domain.AgentType;

public class AgentNotFoundException extends RuntimeException {

    private final AgentType agentType;

    public AgentNotFoundException(AgentType agentType) {
        super("Unknown agent type: " + agentType.value());
        this.agentType = agentType;
    }

    public AgentType agentType() {
        return agentType;
    }
}
