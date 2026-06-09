package com.ai.tts.domain;

import java.util.Map;

public record HealthResponse(
    String status,
    String provider,
    String providerStatus,
    String version,
    Map<String, String> components
) {
    public static final String VERSION = "0.2.0";

    public static HealthResponse healthy(String provider) {
        return new HealthResponse(
            "healthy",
            provider,
            "healthy",
            VERSION,
            Map.of("config", "healthy", "provider", "healthy", "cache", "enabled")
        );
    }

    public static HealthResponse unhealthy(String provider, String reason) {
        return new HealthResponse(
            "unhealthy",
            provider,
            reason,
            VERSION,
            Map.of("config", "healthy", "provider", reason, "cache", "disabled")
        );
    }

    public static HealthResponse degraded(String provider) {
        return new HealthResponse(
            "degraded",
            provider,
            "degraded",
            VERSION,
            Map.of("config", "healthy", "provider", "degraded", "cache", "enabled")
        );
    }
}
