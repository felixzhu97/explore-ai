package com.ai.common.domain.vo;

import java.util.List;
import java.util.Objects;

/** Documentation. */
public record AgentSkill(
    String name,
    String description,
    List<String> allowedTools,
    String instructions,
    String resourceLocation) {
  /** Documentation. */
  public AgentSkill {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(description, "description");
    allowedTools = allowedTools == null ? List.of() : List.copyOf(allowedTools);
    instructions = instructions == null ? "" : instructions;
    Objects.requireNonNull(resourceLocation, "resourceLocation");
  }
}
