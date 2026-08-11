package com.ai.plugin.domain.model;

import com.ai.plugin.domain.vo.PluginHealthStatus;
import com.ai.plugin.domain.vo.PluginInstallationId;

import java.time.Instant;
import java.util.Objects;

public class PluginInstallation {

    public static final String BUILTIN_DEFINITION_ID = "explore-ai";
    public static final String CUSTOM_DEFINITION_ID = "custom";

    private final PluginInstallationId id;
    private final String ownerKey;
    private final String definitionId;
    private String displayName;
    private String endpoint;
    private String authToken;
    private boolean enabled;
    private PluginHealthStatus healthStatus;
    private final Instant createdAt;
    private Instant updatedAt;

    private PluginInstallation(
            PluginInstallationId id,
            String ownerKey,
            String definitionId,
            String displayName,
            String endpoint,
            String authToken,
            boolean enabled,
            PluginHealthStatus healthStatus,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.ownerKey = requireOwnerKey(ownerKey);
        this.definitionId = requireDefinitionId(definitionId);
        this.displayName = requireDisplayName(displayName);
        this.endpoint = normalizeEndpoint(endpoint);
        this.authToken = normalizeToken(authToken);
        this.enabled = enabled;
        this.healthStatus = healthStatus == null ? PluginHealthStatus.UNKNOWN : healthStatus;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static PluginInstallation create(
            String ownerKey,
            String definitionId,
            String displayName,
            String endpoint,
            String authToken) {
        Instant now = Instant.now();
        return new PluginInstallation(
                PluginInstallationId.generate(),
                ownerKey,
                definitionId,
                displayName,
                endpoint,
                authToken,
                true,
                PluginHealthStatus.UNKNOWN,
                now,
                now);
    }

    public static PluginInstallation restore(
            PluginInstallationId id,
            String ownerKey,
            String definitionId,
            String displayName,
            String endpoint,
            String authToken,
            boolean enabled,
            PluginHealthStatus healthStatus,
            Instant createdAt,
            Instant updatedAt) {
        return new PluginInstallation(
                id,
                ownerKey,
                definitionId,
                displayName,
                endpoint,
                authToken,
                enabled,
                healthStatus,
                createdAt,
                updatedAt);
    }

    public boolean isBuiltin() {
        return BUILTIN_DEFINITION_ID.equals(definitionId);
    }

    public boolean isCustom() {
        return CUSTOM_DEFINITION_ID.equals(definitionId);
    }

    public void enable() {
        this.enabled = true;
        touch();
    }

    public void disable() {
        this.enabled = false;
        touch();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        touch();
    }

    public void updateConnection(String endpoint, String authToken) {
        if (isBuiltin()) {
            throw new IllegalStateException("Built-in Plugin has no remote endpoint");
        }
        this.endpoint = normalizeEndpoint(endpoint);
        if (authToken != null) {
            this.authToken = normalizeToken(authToken);
        }
        this.healthStatus = PluginHealthStatus.UNKNOWN;
        touch();
    }

    public void markHealth(PluginHealthStatus status) {
        this.healthStatus = status == null ? PluginHealthStatus.UNKNOWN : status;
        touch();
    }

    public PluginInstallationId getId() {
        return id;
    }

    public String getOwnerKey() {
        return ownerKey;
    }

    public String getDefinitionId() {
        return definitionId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getAuthToken() {
        return authToken;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public PluginHealthStatus getHealthStatus() {
        return healthStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    private static String requireOwnerKey(String ownerKey) {
        if (ownerKey == null || ownerKey.isBlank()) {
            throw new IllegalArgumentException("Owner key is required");
        }
        return ownerKey.trim();
    }

    private static String requireDefinitionId(String definitionId) {
        if (definitionId == null || definitionId.isBlank()) {
            throw new IllegalArgumentException("Definition id is required");
        }
        return definitionId.trim();
    }

    private static String requireDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Display name is required");
        }
        return displayName.trim();
    }

    private static String normalizeEndpoint(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            return null;
        }
        return endpoint.trim();
    }

    private static String normalizeToken(String authToken) {
        if (authToken == null || authToken.isBlank()) {
            return null;
        }
        return authToken.trim();
    }
}
