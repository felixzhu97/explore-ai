package com.ai.text.presentation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Health check response DTO.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record HealthResponse(
        String status,
        String provider,
        String model,
        String version
) {
    public static HealthResponse healthy(String provider, String model) {
        return new HealthResponse("healthy", provider, model, "1.0.0");
    }

    public static HealthResponse unhealthy(String provider, String reason) {
        return new HealthResponse("unhealthy: " + reason, provider, null, "1.0.0");
    }

    public static HealthResponse degraded(String provider, String model, String reason) {
        return new HealthResponse("degraded: " + reason, provider, model, "1.0.0");
    }
}
