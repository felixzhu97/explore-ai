package com.ai.pipeline.web.dto;

import com.ai.pipeline.domain.model.AgentDefinition;

/** Documentation. */
public record AgentHealthResponse(String type, boolean healthy, String status) {
  /** Documentation. */
  public static AgentHealthResponse from(AgentDefinition definition) {
    return new AgentHealthResponse(
        definition.type().value(), definition.healthy(), definition.healthy() ? "UP" : "DOWN");
  }
}
