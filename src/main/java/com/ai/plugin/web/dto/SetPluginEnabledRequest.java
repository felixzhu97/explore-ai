package com.ai.plugin.web.dto;

import jakarta.validation.constraints.NotNull;

public record SetPluginEnabledRequest(@NotNull Boolean enabled) {
}
