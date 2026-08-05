package com.ai.skill.domain.model;

import com.ai.skill.domain.vo.SkillId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Skill {

    private final SkillId id;
    private final String clientId;
    private String name;
    private String description;
    private String instructions;
    private List<String> allowedTools;
    private boolean enabled;
    private final Instant createdAt;
    private Instant updatedAt;

    private Skill(
            SkillId id,
            String clientId,
            String name,
            String description,
            String instructions,
            List<String> allowedTools,
            boolean enabled,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "SkillId cannot be null");
        this.clientId = requireClientId(clientId);
        this.name = requireName(name);
        this.description = normalizeDescription(description);
        this.instructions = requireInstructions(instructions);
        this.allowedTools = copyAllowedTools(allowedTools);
        this.enabled = enabled;
        this.createdAt = Objects.requireNonNull(createdAt, "CreatedAt cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "UpdatedAt cannot be null");
    }

    public static Skill create(
            String clientId,
            String name,
            String description,
            String instructions,
            List<String> allowedTools) {
        Instant now = Instant.now();
        return new Skill(
                SkillId.generate(),
                clientId,
                name,
                description,
                instructions,
                allowedTools,
                true,
                now,
                now);
    }

    public static Skill restore(
            SkillId id,
            String clientId,
            String name,
            String description,
            String instructions,
            List<String> allowedTools,
            boolean enabled,
            Instant createdAt,
            Instant updatedAt) {
        return new Skill(
                id,
                clientId,
                name,
                description,
                instructions,
                allowedTools,
                enabled,
                createdAt,
                updatedAt);
    }

    public Skill update(
            String name,
            String description,
            String instructions,
            List<String> allowedTools) {
        this.name = requireName(name);
        this.description = normalizeDescription(description);
        this.instructions = requireInstructions(instructions);
        this.allowedTools = copyAllowedTools(allowedTools);
        this.updatedAt = Instant.now();
        return this;
    }

    public void enable() {
        this.enabled = true;
        this.updatedAt = Instant.now();
    }

    public void disable() {
        this.enabled = false;
        this.updatedAt = Instant.now();
    }

    public SkillId getId() {
        return id;
    }

    public String getClientId() {
        return clientId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getInstructions() {
        return instructions;
    }

    public List<String> getAllowedTools() {
        return Collections.unmodifiableList(allowedTools);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private static String requireClientId(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("ClientId cannot be null or blank");
        }
        return clientId.trim();
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Skill name cannot be null or blank");
        }
        String trimmed = name.trim();
        if (trimmed.length() > 120) {
            throw new IllegalArgumentException("Skill name cannot exceed 120 characters");
        }
        return trimmed;
    }

    private static String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return "";
        }
        String trimmed = description.trim();
        return trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed;
    }

    private static String requireInstructions(String instructions) {
        if (instructions == null || instructions.isBlank()) {
            throw new IllegalArgumentException("Skill instructions cannot be null or blank");
        }
        return instructions.trim();
    }

    private static List<String> copyAllowedTools(List<String> allowedTools) {
        if (allowedTools == null || allowedTools.isEmpty()) {
            return List.of();
        }
        return Collections.unmodifiableList(new ArrayList<>(allowedTools));
    }
}
