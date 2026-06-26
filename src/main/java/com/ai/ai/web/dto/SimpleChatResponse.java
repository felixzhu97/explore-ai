package com.ai.ai.web.dto;

/**
 * Simple chat response DTO for legacy API compatibility.
 */
@Deprecated
public record SimpleChatResponse(
    String message,
    String sessionId
) {
    public static SimpleChatResponse of(String message) {
        return new SimpleChatResponse(message, null);
    }

    public static SimpleChatResponse of(String message, String sessionId) {
        return new SimpleChatResponse(message, sessionId);
    }
}
