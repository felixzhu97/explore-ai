package com.ai.automation.domain.vo;

import java.util.Objects;
import java.util.UUID;

public record RunId(String value) {

    public RunId {
        Objects.requireNonNull(value, "RunId cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("RunId cannot be blank");
        }
    }

    public static RunId generate() {
        return new RunId(UUID.randomUUID().toString());
    }

    public static RunId of(String value) {
        return new RunId(value);
    }
}
