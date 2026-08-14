package com.ai.automation.web.dto;

import jakarta.validation.constraints.NotNull;

/** Documentation. */
public record SetAutomationEnabledRequest(@NotNull Boolean enabled) {}
