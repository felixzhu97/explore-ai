package com.ai.pipeline.domain.vo;

import java.util.UUID;

public record SavedAgentId(String value) {

    public SavedAgentId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("SavedAgentId cannot be null or blank");
        }
    }

    public static SavedAgentId of(String value) {
        return new SavedAgentId(value);
    }

    public static SavedAgentId generate() {
        return new SavedAgentId(UUID.randomUUID().toString());
    }
}
