package com.ai.ai.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Chat request DTO for API requests.
 */
public record ChatRequest(

    @Size(max = 10000, message = "Message cannot exceed 10000 characters")
    String message,

    @NotBlank(message = "Session ID cannot be blank")
    String sessionId
) {
    public ChatRequest {
        if (message != null) {
            message = message.trim();
        }
    }

    public static ChatRequest of(String message) {
        return new ChatRequest(message, null);
    }

    public static ChatRequest of(String message, String sessionId) {
        return new ChatRequest(message, sessionId);
    }
}
