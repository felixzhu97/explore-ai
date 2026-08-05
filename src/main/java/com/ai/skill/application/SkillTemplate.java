package com.ai.skill.application;

public record SkillTemplate(
        String id,
        String name,
        String description,
        String instructions,
        java.util.List<String> allowedTools
) {}
