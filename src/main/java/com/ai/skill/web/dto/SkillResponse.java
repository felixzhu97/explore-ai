package com.ai.skill.web.dto;

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
        Instant updatedAt
) {
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
