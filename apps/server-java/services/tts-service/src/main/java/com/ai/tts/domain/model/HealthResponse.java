package com.ai.tts.domain.model;

public record HealthResponse(
    String status,
    String provider,
    String providerStatus,
    String version
) {
    public static HealthResponse healthy(String provider) {
        return new HealthResponse("healthy", provider, "healthy", "1.0.0");
    }

    public static HealthResponse unhealthy(String provider, String reason) {
        return new HealthResponse("unhealthy", provider, reason, "1.0.0");
    }
}
