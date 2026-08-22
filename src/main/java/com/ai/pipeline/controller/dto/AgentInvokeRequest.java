package com.ai.pipeline.controller.dto;

import jakarta.validation.constraints.NotBlank;

/** Documentation. */
public record AgentInvokeRequest(@NotBlank String message, String sessionId, String agentType) {}
