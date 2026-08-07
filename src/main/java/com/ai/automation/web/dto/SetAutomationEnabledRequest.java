package com.ai.automation.web.dto;

import jakarta.validation.constraints.NotNull;

public record SetAutomationEnabledRequest(@NotNull Boolean enabled) {
}
