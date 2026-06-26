package com.ai.chat.domain.vo;

import java.util.UUID;

/**
 * Strongly-typed ID for ChatSession.
 */
public record ChatSessionId(String value) {

    public ChatSessionId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ChatSessionId cannot be null or blank");
        }
    }

    public static ChatSessionId of(String value) {
        return new ChatSessionId(value);
    }

    public static ChatSessionId generate() {
        return new ChatSessionId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return value;
    }
}
