package com.ai.skill.controller.dto;

import com.ai.skill.domain.model.Skill;
import java.time.Instant;
import java.util.List;

/** Documentation. */
public record SkillResponse(
    String id,
    String name,
    String description,
    String instructions,
    List<String> allowedTools,
    boolean enabled,
    Instant createdAt,
    Instant updatedAt) {
  /** Documentation. */
  public static SkillResponse from(Skill skill) {
    return new SkillResponse(
        skill.getId().value(),
        skill.getName(),
        skill.getDescription(),
        skill.getInstructions(),
        skill.getAllowedTools(),
        skill.isEnabled(),
        skill.getCreatedAt(),
        skill.getUpdatedAt());
  }
}
