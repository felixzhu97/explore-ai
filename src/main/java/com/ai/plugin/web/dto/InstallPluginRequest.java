package com.ai.plugin.web.dto;

import jakarta.validation.constraints.NotBlank;

public record InstallPluginRequest(
        @NotBlank String definitionId,
        String endpoint,
        String authToken,
        String customName
) {
}
