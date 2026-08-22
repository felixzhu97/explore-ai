package com.ai.pipeline.controller.dto;

import com.ai.pipeline.domain.model.AgentDefinition;
import java.util.List;

/** Documentation. */
public record AgentInfoResponse(
    String type,
    String name,
    String description,
    boolean healthy,
    boolean supervisor,
    String runtime,
    List<String> toolKeys,
    String systemPrompt) {
  /** Documentation. */
  public static AgentInfoResponse from(AgentDefinition definition) {
    return new AgentInfoResponse(
        definition.type().value(),
        definition.name(),
        definition.description(),
        definition.healthy(),
        definition.type().isSupervisor(),
        definition.runtime(),
        definition.toolKeys(),
        definition.systemPrompt());
  }
}
