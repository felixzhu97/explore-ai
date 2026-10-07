package com.ai.pipeline.controller.dto;

import com.ai.pipeline.domain.model.AgentDefinition;
import java.util.List;

public record AgentInfoResponse(
    String type,
    String name,
    String description,
    boolean healthy,
    boolean supervisor,
    AgentRuntime runtime,
    List<String> toolKeys,
    String systemPrompt) {
  /** Builds a response from an agent definition, flagging whether it is the supervisor. */
  public static AgentInfoResponse createResponse(AgentDefinition definition) {
    return new AgentInfoResponse(
        definition.getType().getValue(),
        definition.getName(),
        definition.getDescription(),
        definition.isHealthy(),
        definition.getType().isSupervisor(),
        AgentRuntime.createResponse(definition.getRuntime()),
        definition.getToolKeys(),
        definition.getSystemPrompt());
  }
}
