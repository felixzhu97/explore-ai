package com.ai.pipeline.controller.dto;

import com.ai.common.controller.dto.HealthStatus;
import com.ai.pipeline.domain.model.AgentDefinition;

public record AgentHealthResponse(String type, boolean healthy, HealthStatus status) {
  public static AgentHealthResponse from(AgentDefinition definition) {
    return new AgentHealthResponse(
        definition.type().value(), definition.healthy(), HealthStatus.of(definition.healthy()));
  }
}
