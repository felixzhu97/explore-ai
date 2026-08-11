package com.ai.plugin.web.dto;

import com.ai.plugin.domain.model.PluginInstallation;

public record PluginInstallationResponse(
        String id,
        String definitionId,
        String displayName,
        String endpoint,
        boolean enabled,
        String healthStatus,
        boolean builtin,
        boolean hasAuthToken,
        String createdAt,
        String updatedAt
) {
    public static PluginInstallationResponse from(PluginInstallation installation) {
        return new PluginInstallationResponse(
                installation.getId().value(),
                installation.getDefinitionId(),
                installation.getDisplayName(),
                installation.getEndpoint(),
                installation.isEnabled(),
                installation.getHealthStatus().name(),
                installation.isBuiltin(),
                installation.getAuthToken() != null && !installation.getAuthToken().isBlank(),
                installation.getCreatedAt().toString(),
                installation.getUpdatedAt().toString());
    }
}
