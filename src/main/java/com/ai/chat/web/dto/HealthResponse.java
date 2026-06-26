package com.ai.chat.web.dto;

/**
 * Health check response DTO.
 */
public record HealthResponse(
    String status
) {
    public static HealthResponse up() {
        return new HealthResponse("UP");
    }
}
