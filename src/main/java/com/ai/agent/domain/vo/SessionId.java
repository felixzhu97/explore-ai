package com.ai.agent.domain.vo;

import java.util.Objects;
import java.util.UUID;

public record SessionId(String value) {

    public SessionId {
        Objects.requireNonNull(value, "session id must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("session id must not be blank");
        }
        value = value.trim();
    }

    public static SessionId generate() {
        return new SessionId(UUID.randomUUID().toString());
    }

    public static SessionId of(String value) {
        return new SessionId(value);
    }

    /** ChatMemory / ToolEventChannel conversation key. */
    public String conversationId() {
        return "agent:" + value;
    }
}
