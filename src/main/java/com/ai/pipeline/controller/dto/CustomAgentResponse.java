package com.ai.pipeline.controller.dto;

import com.ai.pipeline.domain.model.CustomAgent;
import java.time.Instant;
import java.util.List;

public record CustomAgentResponse(
    String id,
    String agentType,
    String name,
    String description,
    String systemPrompt,
    List<String> tools,
    boolean enabled,
    Instant createdAt,
    Instant updatedAt) {
  /** Builds a response from a saved library agent definition. */
  public static CustomAgentResponse createResponse(CustomAgent agent) {
    return new CustomAgentResponse(
        agent.getId().toString(),
        agent.getAgentType(),
        agent.getName(),
        agent.getDescription(),
        agent.getSystemPrompt(),
        List.copyOf(agent.getTools()),
        agent.isEnabled(),
        agent.getCreatedAt(),
        agent.getUpdatedAt());
  }
}
