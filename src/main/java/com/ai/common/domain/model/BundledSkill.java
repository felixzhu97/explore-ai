package com.ai.common.domain.model;

import java.util.List;
import java.util.Objects;
import lombok.Value;

/** Skill shipped with the app and loaded from a bundled resource. */
@Value
public class BundledSkill {
  String name;
  String description;
  List<String> allowedTools;
  String instructions;
  String resourceLocation;

  public BundledSkill(
      String name,
      String description,
      List<String> allowedTools,
      String instructions,
      String resourceLocation) {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(description, "description");
    allowedTools = allowedTools == null ? List.of() : List.copyOf(allowedTools);
    instructions = instructions == null ? "" : instructions;
    Objects.requireNonNull(resourceLocation, "resourceLocation");
    this.name = name;
    this.description = description;
    this.allowedTools = allowedTools;
    this.instructions = instructions;
    this.resourceLocation = resourceLocation;
  }
}
