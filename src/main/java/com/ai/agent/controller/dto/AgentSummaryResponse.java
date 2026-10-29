package com.ai.agent.controller.dto;

import com.ai.pipeline.domain.model.AgentDefinition;
import java.util.List;

/** Sibling-facing Agent summary (no system prompt — opaque Published Language). */
public record AgentSummaryResponse(
    String type, String name, String description, boolean healthy, List<String> tools) {

  /** Maps a Pipeline Agent Definition to the published summary. */
  public static AgentSummaryResponse createResponse(AgentDefinition definition) {
    return new AgentSummaryResponse(
        definition.getType().getValue(),
        definition.getName(),
        definition.getDescription(),
        definition.isHealthy(),
        definition.getTools());
  }
}
