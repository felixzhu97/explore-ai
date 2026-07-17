package com.ai.agent.domain.model;

import com.ai.agent.domain.vo.AgentType;

import java.util.Objects;

/**
 * Immutable definition of a specialized or supervisor agent.
 */
public final class AgentDefinition {

    private final AgentType type;
    private final String name;
    private final String description;
    private final String systemPrompt;
    private final boolean healthy;

    private AgentDefinition(
            AgentType type,
            String name,
            String description,
            String systemPrompt,
            boolean healthy) {
        this.type = Objects.requireNonNull(type, "type");
        this.name = Objects.requireNonNull(name, "name");
        this.description = Objects.requireNonNull(description, "description");
        this.systemPrompt = Objects.requireNonNull(systemPrompt, "systemPrompt");
        this.healthy = healthy;
    }

    public static AgentDefinition create(
            AgentType type,
            String name,
            String description,
            String systemPrompt) {
        return new AgentDefinition(type, name, description, systemPrompt, true);
    }

    public AgentType type() {
        return type;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public String systemPrompt() {
        return systemPrompt;
    }

    public boolean healthy() {
        return healthy;
    }

    public boolean isWorker() {
        return !type.isSupervisor();
    }
}
