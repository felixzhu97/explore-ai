package com.ai.agents.domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Result from a tool execution.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ToolResult(
    /**
     * Name of the executed tool.
     */
    String toolName,

    /**
     * Tool execution result.
     */
    String result,

    /**
     * Whether the tool execution succeeded.
     */
    boolean success,

    /**
     * Error message if execution failed.
     */
    String error
) {
    public static ToolResult success(String toolName, String result) {
        return new ToolResult(toolName, result, true, null);
    }

    public static ToolResult failure(String toolName, String error) {
        return new ToolResult(toolName, null, false, error);
    }
}
