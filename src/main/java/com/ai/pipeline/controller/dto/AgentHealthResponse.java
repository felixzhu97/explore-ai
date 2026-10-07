package com.ai.pipeline.controller.dto;

import com.ai.common.controller.dto.HealthStatus;
import com.ai.pipeline.domain.model.AgentDefinition;

public record AgentHealthResponse(String type, boolean healthy, HealthStatus status) {
  /** Maps an agent definition to a health response. */
  public static AgentHealthResponse from(AgentDefinition definition) {
    return new AgentHealthResponse(
        definition.getType().value(),
        definition.isHealthy(),
        HealthStatus.of(definition.isHealthy()));
  }
}
