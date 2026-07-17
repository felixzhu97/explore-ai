package com.ai.agent.web.dto;

import com.ai.agent.domain.model.AgentDefinition;

public record AgentInfoResponse(
        String type,
        String name,
        String description,
        boolean healthy,
        boolean supervisor) {

    public static AgentInfoResponse from(AgentDefinition definition) {
        return new AgentInfoResponse(
                definition.type().value(),
                definition.name(),
                definition.description(),
                definition.healthy(),
                definition.type().isSupervisor());
    }
}
