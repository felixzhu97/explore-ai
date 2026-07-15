package com.ai.agent.domain.vo;

import java.util.Locale;
import java.util.Objects;

/**
 * Identifier for a registered agent (supervisor or specialized worker).
 */
public record AgentType(String value) {

    public AgentType {
        Objects.requireNonNull(value, "agent type must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("agent type must not be blank");
        }
        value = value.trim().toLowerCase(Locale.ROOT);
    }

    public static AgentType of(String value) {
        return new AgentType(value);
    }

    public static AgentType supervisor() {
        return new AgentType("supervisor");
    }

    public boolean isSupervisor() {
        return "supervisor".equals(value);
    }
}
