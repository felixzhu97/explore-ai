package com.ai.pipeline.controller.dto;

import com.ai.pipeline.domain.model.AgentDefinition;
import java.util.List;

public record AgentInfoResponse(
    String type,
    String name,
    String description,
    boolean healthy,
    boolean supervisor,
    String runtime,
    List<String> toolKeys,
    String systemPrompt) {
  /** Builds a response from an agent definition, flagging whether it is the supervisor. */
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
