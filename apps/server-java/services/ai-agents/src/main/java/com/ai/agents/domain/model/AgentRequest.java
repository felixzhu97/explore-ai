package com.ai.agents.domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

/**
 * Request model for agent processing.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AgentRequest(
    /**
     * The user's message content.
     */
    @NotBlank(message = "Message content is required")
    String message,

    /**
     * Optional conversation history for context.
     */
    List<ChatMessage> history,

    /**
     * Optional context data passed to the agent.
     */
    Map<String, Object> context,

    /**
     * Optional session ID for conversation tracking.
     */
    String sessionId
) {
    public AgentRequest(String message) {
        this(message, null, null, null);
    }

    public AgentRequest(String message, List<ChatMessage> history) {
        this(message, history, null, null);
    }
}
