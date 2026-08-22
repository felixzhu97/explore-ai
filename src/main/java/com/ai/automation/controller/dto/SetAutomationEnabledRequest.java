package com.ai.automation.controller.dto;

import jakarta.validation.constraints.NotNull;

/** Documentation. */
public record SetAutomationEnabledRequest(@NotNull Boolean enabled) {}
