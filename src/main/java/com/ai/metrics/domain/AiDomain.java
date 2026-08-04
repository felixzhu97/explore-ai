package com.ai.metrics.domain;

import java.util.Locale;
import java.util.Optional;

/**
 * Business domains that emit AI invocation events for metrics.
 */
public enum AiDomain {
    CHAT("chat"),
    RAG("rag"),
    AGENTS("agents"),
    TOOLS("tools"),
    VISION("vision"),
    WORKFLOW("workflow");

    private final String value;

    AiDomain(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static Optional<AiDomain> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (AiDomain domain : values()) {
            if (domain.value.equals(normalized)) {
                return Optional.of(domain);
            }
        }
        return Optional.empty();
    }

    public static AiDomain require(String raw) {
        return parse(raw).orElseThrow(() -> new IllegalArgumentException("Unknown AI domain: " + raw));
    }
}
