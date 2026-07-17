package com.ai.agent.web.dto;

import com.ai.agent.domain.model.AgentDefinition;

public record AgentHealthResponse(
        String type,
        boolean healthy,
        String status) {

    public static AgentHealthResponse from(AgentDefinition definition) {
        return new AgentHealthResponse(
                definition.type().value(),
                definition.healthy(),
                definition.healthy() ? "UP" : "DOWN");
    }
}
