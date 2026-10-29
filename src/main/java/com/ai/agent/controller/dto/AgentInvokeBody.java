package com.ai.agent.controller.dto;

import jakarta.validation.constraints.NotBlank;

/** Request body for baseline Agent invoke. */
public record AgentInvokeBody(@NotBlank String message) {}
