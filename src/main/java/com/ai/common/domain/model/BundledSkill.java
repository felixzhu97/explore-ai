package com.ai.common.domain.model;

import java.util.List;
import java.util.Objects;

public record BundledSkill(
    String name,
    String description,
    List<String> allowedTools,
    String instructions,
    String resourceLocation) {
  public BundledSkill {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(description, "description");
    allowedTools = allowedTools == null ? List.of() : List.copyOf(allowedTools);
    instructions = instructions == null ? "" : instructions;
    Objects.requireNonNull(resourceLocation, "resourceLocation");
  }
}
