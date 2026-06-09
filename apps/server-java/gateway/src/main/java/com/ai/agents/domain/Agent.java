package com.ai.agents.domain;

import java.util.Objects;
import java.util.Optional;

/**
 * Agent entity - aggregate root representing an AI agent.
 * Implemented as a rich domain model (DDD anemic model anti-pattern avoided).
 */
public final class Agent {
    private final AgentId id;
    private final AgentName name;
    private final AgentType type;
    private final AgentCapabilities capabilities;
    private final String description;

    private Agent(
            AgentId id,
            AgentName name,
            AgentType type,
            AgentCapabilities capabilities,
            String description
    ) {
        this.id = Objects.requireNonNull(id, "AgentId cannot be null");
        this.name = Objects.requireNonNull(name, "AgentName cannot be null");
        this.type = Objects.requireNonNull(type, "AgentType cannot be null");
        this.capabilities = Objects.requireNonNull(capabilities, "Capabilities cannot be null");
        this.description = description != null ? description : "";
        validateInvariants();
    }

    private void validateInvariants() {
        if (capabilities == null || capabilities.equals(AgentCapabilities.none())) {
            throw new IllegalStateException("Agent must have at least one capability");
        }
    }

    /**
     * Factory method to create a new Agent.
     */
    public static Agent create(
            AgentId id,
            AgentName name,
            AgentType type,
            AgentCapabilities capabilities
    ) {
        return create(id, name, type, capabilities, "");
    }

    /**
     * Factory method to create a new Agent with description.
     */
    public static Agent create(
            AgentId id,
            AgentName name,
            AgentType type,
            AgentCapabilities capabilities,
            String description
    ) {
        if (id == null || name == null || type == null || capabilities == null) {
            throw new IllegalArgumentException("Required parameters cannot be null");
        }
        return new Agent(id, name, type, capabilities, description);
    }

    /**
     * Creates a Chat agent.
     */
    public static Agent chatAgent(String id, String name) {
        return create(
                AgentId.of(id),
                AgentName.of(name),
                AgentType.CHAT,
                AgentCapabilities.of(AgentType.CHAT)
        );
    }

    /**
     * Creates a RAG agent.
     */
    public static Agent ragAgent(String id, String name) {
        return create(
                AgentId.of(id),
                AgentName.of(name),
                AgentType.RAG,
                AgentCapabilities.of(AgentType.RAG)
        );
    }

    /**
     * Creates a TTS agent.
     */
    public static Agent ttsAgent(String id, String name) {
        return create(
                AgentId.of(id),
                AgentName.of(name),
                AgentType.TTS,
                AgentCapabilities.of(AgentType.TTS)
        );
    }

    /**
     * Creates a Vision agent.
     */
    public static Agent visionAgent(String id, String name) {
        return create(
                AgentId.of(id),
                AgentName.of(name),
                AgentType.VISION,
                AgentCapabilities.of(AgentType.VISION)
        );
    }

    /**
     * Creates a Media agent.
     */
    public static Agent mediaAgent(String id, String name) {
        return create(
                AgentId.of(id),
                AgentName.of(name),
                AgentType.MEDIA,
                AgentCapabilities.of(AgentType.MEDIA)
        );
    }

    /**
     * Creates a Text agent.
     */
    public static Agent textAgent(String id, String name) {
        return create(
                AgentId.of(id),
                AgentName.of(name),
                AgentType.TEXT,
                AgentCapabilities.of(AgentType.TEXT)
        );
    }

    /**
     * Creates a Supervisor agent.
     */
    public static Agent supervisorAgent(String id, String name) {
        return create(
                AgentId.of(id),
                AgentName.of(name),
                AgentType.SUPERVISOR,
                AgentCapabilities.all()
        );
    }

    // Business methods

    /**
     * Check if this agent can handle the given agent type.
     */
    public boolean canHandle(AgentType type) {
        return capabilities.supports(type);
    }

    /**
     * Check if this agent supports the given agent type.
     */
    public boolean supports(AgentType type) {
        return this.type == type || capabilities.supports(type);
    }

    /**
     * Check if this agent can execute the given conversation.
     */
    public boolean canExecute(Conversation conversation) {
        return conversation != null && canContinue(conversation);
    }

    /**
     * Check if the conversation can continue.
     */
    public boolean canContinue(Conversation conversation) {
        return conversation.canContinue() && supports(conversation.primaryAgentId() == null ? AgentType.CHAT : AgentType.CHAT);
    }

    // Getters

    public AgentId id() { return id; }
    public AgentName name() { return name; }
    public AgentType type() { return type; }
    public AgentCapabilities capabilities() { return capabilities; }
    public String description() { return description; }

    public String getIdValue() { return id.value(); }
    public String getNameValue() { return name.value(); }
    public String getTypeId() { return type.getId(); }

    // Value equality

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Agent agent = (Agent) o;
        return id.equals(agent.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Agent{" +
                "id=" + id +
                ", name=" + name +
                ", type=" + type +
                '}';
    }
}
