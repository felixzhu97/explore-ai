package com.ai.agents.domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Information about a registered agent.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AgentInfo(
    /**
     * Unique agent identifier.
     */
    String name,

    /**
     * Agent display name.
     */
    String displayName,

    /**
     * Agent description.
     */
    String description,

    /**
     * Current agent status.
     */
    String status,

    /**
     * Agent type.
     */
    String type
) {
    public static AgentInfo of(String name, String displayName, String description, String status, String type) {
        return new AgentInfo(name, displayName, description, status, type);
    }
}
