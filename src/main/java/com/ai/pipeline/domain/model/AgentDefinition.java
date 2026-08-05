package com.ai.pipeline.domain.model;

import com.ai.pipeline.domain.vo.AgentType;

import java.util.List;
import java.util.Objects;

/**
 * Immutable definition of a specialized or supervisor agent.
 */
public final class AgentDefinition {

    public static final String RUNTIME_SINGLE = "single";
    public static final String RUNTIME_DEEP = "deep";

    private final AgentType type;
    private final String name;
    private final String description;
    private final String systemPrompt;
    private final List<String> toolKeys;
    private final String runtime;
    private final boolean healthy;

    private AgentDefinition(
            AgentType type,
            String name,
            String description,
            String systemPrompt,
            List<String> toolKeys,
            String runtime,
            boolean healthy) {
        this.type = Objects.requireNonNull(type, "type");
        this.name = Objects.requireNonNull(name, "name");
        this.description = Objects.requireNonNull(description, "description");
        this.systemPrompt = Objects.requireNonNull(systemPrompt, "systemPrompt");
        this.toolKeys = toolKeys == null ? List.of() : List.copyOf(toolKeys);
        this.runtime = runtime == null || runtime.isBlank() ? RUNTIME_SINGLE : runtime.trim().toLowerCase();
        this.healthy = healthy;
    }

    public static AgentDefinition create(
            AgentType type,
            String name,
            String description,
            String systemPrompt) {
        return create(type, name, description, systemPrompt, List.of(), RUNTIME_SINGLE);
    }

    public static AgentDefinition create(
            AgentType type,
            String name,
            String description,
            String systemPrompt,
            List<String> toolKeys,
            String runtime) {
        return new AgentDefinition(type, name, description, systemPrompt, toolKeys, runtime, true);
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

    public List<String> toolKeys() {
        return toolKeys;
    }

    public String runtime() {
        return runtime;
    }

    public boolean healthy() {
        return healthy;
    }

    public boolean isWorker() {
        return !type.isSupervisor() && !isDeep();
    }

    public boolean isDeep() {
        return RUNTIME_DEEP.equals(runtime) || "deep".equals(type.value());
    }
}
