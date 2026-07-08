package com.ai.analysis.domain.model;

public enum Sentiment {
    POSITIVE,
    NEUTRAL,
    NEGATIVE;

    public static Sentiment fromString(String value) {
        if (value == null || value.isBlank()) {
            return NEUTRAL;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return NEUTRAL;
        }
    }

    public boolean isNegative() {
        return this == NEGATIVE;
    }

    public boolean isPositive() {
        return this == POSITIVE;
    }
}
