package com.ai.agents.domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/**
 * Response model for agent processing.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AgentResponse(
    /**
     * The agent's response content.
     */
    String content,

    /**
     * The agent that processed the request.
     */
    String agentId,

    /**
     * The agent type that processed the request.
     */
    String agentType,

    /**
     * Optional tool execution results.
     */
    List<ToolResult> toolResults,

    /**
     * Optional metadata about the processing.
     */
    Map<String, Object> metadata,

    /**
     * Whether the request was successful.
     */
    boolean success,

    /**
     * Error message if processing failed.
     */
    String error
) {
    /**
     * Create a successful response.
     */
    public static AgentResponse success(String content, String agentId, String agentType) {
        return new AgentResponse(content, agentId, agentType, null, null, true, null);
    }

    /**
     * Create a successful response with tool results.
     */
    public static AgentResponse success(String content, String agentId, String agentType, List<ToolResult> toolResults) {
        return new AgentResponse(content, agentId, agentType, toolResults, null, true, null);
    }

    /**
     * Create an error response.
     */
    public static AgentResponse error(String errorMessage) {
        return new AgentResponse(null, null, null, null, null, false, errorMessage);
    }
}
