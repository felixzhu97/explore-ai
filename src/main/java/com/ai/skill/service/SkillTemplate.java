package com.ai.skill.service;

public record SkillTemplate(
    String id,
    String name,
    String description,
    String instructions,
    java.util.List<String> allowedTools) {}
