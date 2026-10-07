package com.ai.skill.controller.dto;

import com.ai.skill.domain.model.Skill;
import java.time.Instant;
import java.util.List;

public record SkillResponse(
    String id,
    String name,
    String description,
    String instructions,
    List<String> allowedTools,
    boolean enabled,
    Instant createdAt,
    Instant updatedAt) {
  /** Maps a {@code Skill} aggregate to its API response. */
  public static SkillResponse from(Skill skill) {
    return new SkillResponse(
        skill.getId().toString(),
        skill.getName(),
        skill.getDescription(),
        skill.getInstructions(),
        skill.getAllowedTools(),
        skill.isEnabled(),
        skill.getCreatedAt(),
        skill.getUpdatedAt());
  }
}
