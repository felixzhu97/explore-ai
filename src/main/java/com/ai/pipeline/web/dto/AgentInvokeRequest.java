package com.ai.pipeline.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Documentation. */
public record AgentInvokeRequest(@NotBlank String message, String sessionId, String agentType) {}
