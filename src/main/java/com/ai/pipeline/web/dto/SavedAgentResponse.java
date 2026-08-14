package com.ai.pipeline.web.dto;

import com.ai.pipeline.domain.model.SavedAgentDefinition;
import java.time.Instant;
import java.util.List;

/** Documentation. */
public record SavedAgentResponse(
    String id,
    String typeKey,
    String name,
    String description,
    String systemPrompt,
    List<String> toolKeys,
    boolean enabled,
    Instant createdAt,
    Instant updatedAt) {
  /** Documentation. */
  public static SavedAgentResponse from(SavedAgentDefinition agent) {
    return new SavedAgentResponse(
        agent.getId().value(),
        agent.getTypeKey(),
        agent.getName(),
        agent.getDescription(),
        agent.getSystemPrompt(),
        List.copyOf(agent.getToolKeys()),
        agent.isEnabled(),
        agent.getCreatedAt(),
        agent.getUpdatedAt());
  }
}
