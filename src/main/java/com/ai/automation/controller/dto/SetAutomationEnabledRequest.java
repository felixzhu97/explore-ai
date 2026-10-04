package com.ai.automation.controller.dto;

import jakarta.validation.constraints.NotNull;

public record SetAutomationEnabledRequest(@NotNull Boolean enabled) {}
