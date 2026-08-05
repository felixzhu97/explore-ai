package com.ai.pipeline.domain.model;

import com.ai.pipeline.domain.vo.SavedAgentId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public class SavedAgentDefinition {

    private static final Pattern TYPE_KEY_PATTERN = Pattern.compile("^[a-z][a-z0-9_-]{0,63}$");

    private final SavedAgentId id;
    private final String clientId;
    private String typeKey;
    private String name;
    private String description;
    private String systemPrompt;
    private List<String> toolKeys;
    private boolean enabled;
    private final Instant createdAt;
    private Instant updatedAt;

    private SavedAgentDefinition(
            SavedAgentId id,
            String clientId,
            String typeKey,
            String name,
            String description,
            String systemPrompt,
            List<String> toolKeys,
            boolean enabled,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "SavedAgentId cannot be null");
        this.clientId = requireClientId(clientId);
        this.typeKey = requireTypeKey(typeKey);
        this.name = requireName(name);
        this.description = normalizeDescription(description);
        this.systemPrompt = requireSystemPrompt(systemPrompt);
        this.toolKeys = copyToolKeys(toolKeys);
        this.enabled = enabled;
        this.createdAt = Objects.requireNonNull(createdAt, "CreatedAt cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "UpdatedAt cannot be null");
    }

    public static SavedAgentDefinition create(
            String clientId,
            String typeKey,
            String name,
            String description,
            String systemPrompt,
            List<String> toolKeys) {
        Instant now = Instant.now();
        return new SavedAgentDefinition(
                SavedAgentId.generate(),
                clientId,
                typeKey,
                name,
                description,
                systemPrompt,
                toolKeys,
                true,
                now,
                now);
    }

    public static SavedAgentDefinition restore(
            SavedAgentId id,
            String clientId,
            String typeKey,
            String name,
            String description,
            String systemPrompt,
            List<String> toolKeys,
            boolean enabled,
            Instant createdAt,
            Instant updatedAt) {
        return new SavedAgentDefinition(
                id,
                clientId,
                typeKey,
                name,
                description,
                systemPrompt,
                toolKeys,
                enabled,
                createdAt,
                updatedAt);
    }

    public SavedAgentDefinition update(
            String name,
            String description,
            String systemPrompt,
            List<String> toolKeys) {
        this.name = requireName(name);
        this.description = normalizeDescription(description);
        this.systemPrompt = requireSystemPrompt(systemPrompt);
        this.toolKeys = copyToolKeys(toolKeys);
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

    public SavedAgentId getId() {
        return id;
    }

    public String getClientId() {
        return clientId;
    }

    public String getTypeKey() {
        return typeKey;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public List<String> getToolKeys() {
        return Collections.unmodifiableList(toolKeys);
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

    public AgentDefinition toAgentDefinition() {
        return AgentDefinition.create(
                com.ai.pipeline.domain.vo.AgentType.of(typeKey),
                name,
                description,
                systemPrompt,
                toolKeys,
                AgentDefinition.RUNTIME_SINGLE);
    }

    private static String requireClientId(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId cannot be blank");
        }
        return clientId.trim();
    }

    private static String requireTypeKey(String typeKey) {
        if (typeKey == null || typeKey.isBlank()) {
            throw new IllegalArgumentException("typeKey cannot be blank");
        }
        String normalized = typeKey.trim().toLowerCase(Locale.ROOT);
        if (!TYPE_KEY_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("typeKey must be lowercase alphanumeric with _ or -");
        }
        if ("supervisor".equals(normalized)) {
            throw new IllegalArgumentException("typeKey cannot be supervisor");
        }
        return normalized;
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name cannot be blank");
        }
        return name.trim();
    }

    private static String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return "";
        }
        String trimmed = description.trim();
        return trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed;
    }

    private static String requireSystemPrompt(String systemPrompt) {
        if (systemPrompt == null || systemPrompt.isBlank()) {
            throw new IllegalArgumentException("systemPrompt cannot be blank");
        }
        return systemPrompt.trim();
    }

    private static List<String> copyToolKeys(List<String> toolKeys) {
        if (toolKeys == null || toolKeys.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> copy = new ArrayList<>();
        for (String key : toolKeys) {
            if (key != null && !key.isBlank()) {
                copy.add(key.trim());
            }
        }
        return copy;
    }
}
