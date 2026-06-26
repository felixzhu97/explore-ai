package com.ai.chat.web.dto;

import com.ai.chat.domain.model.ChatMessage;

import java.time.Instant;

/**
 * Chat response DTO for API responses.
 */
public record ChatResponse(
    String response,
    String sessionId,
    String messageId,
    Instant timestamp
) {

    public static ChatResponse of(String response, String sessionId, String messageId) {
        return new ChatResponse(response, sessionId, messageId, Instant.now());
    }

    public static ChatResponse of(String response) {
        return new ChatResponse(response, null, null, Instant.now());
    }

    public static ChatResponse fromMessage(ChatMessage message, String sessionId) {
        return new ChatResponse(
            message.getText(),
            sessionId,
            message.getId().toString(),
            message.getTimestamp()
        );
    }
}
