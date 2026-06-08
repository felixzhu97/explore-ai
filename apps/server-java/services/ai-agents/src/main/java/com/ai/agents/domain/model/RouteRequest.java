package com.ai.agents.domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;

/**
 * Request model for supervisor routing.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RouteRequest(
    /**
     * The message to route.
     */
    @NotBlank(message = "Message is required")
    String message,

    /**
     * Optional context for routing decision.
     */
    String context
) {
}
