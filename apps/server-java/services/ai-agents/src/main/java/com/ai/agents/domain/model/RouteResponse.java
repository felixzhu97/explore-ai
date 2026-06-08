package com.ai.agents.domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response model for supervisor routing.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RouteResponse(
    /**
     * The target agent ID.
     */
    String targetAgent,

    /**
     * Reason for the routing decision.
     */
    String reason,

    /**
     * Confidence score for the routing (0.0 - 1.0).
     */
    Double confidence,

    /**
     * Alternative agents that could handle the request.
     */
    java.util.List<String> alternatives
) {
    public static RouteResponse of(String targetAgent, String reason) {
        return new RouteResponse(targetAgent, reason, null, null);
    }

    public static RouteResponse of(String targetAgent, String reason, double confidence) {
        return new RouteResponse(targetAgent, reason, confidence, null);
    }
}
