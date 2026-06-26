package com.ai.chat.domain.vo;

import java.util.UUID;

/**
 * Message ID value object ensuring type safety for message identifiers.
 */
public record MessageId(String value) {

    public MessageId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("MessageId cannot be null or blank");
        }
    }

    public static MessageId of(String value) {
        return new MessageId(value);
    }

    public static MessageId generate() {
        return new MessageId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return value;
    }
}
