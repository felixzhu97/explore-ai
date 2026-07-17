package com.ai.agent.web.dto;

import jakarta.validation.constraints.NotBlank;

public record AgentInvokeRequest(
        @NotBlank String message,
        String sessionId,
        String agentType) {
}
