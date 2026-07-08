package com.ai.mcp.domain.model;

import com.ai.mcp.domain.exception.McpToolNotFoundException;

public record McpToolDefinition(String name, String description) {

    public static McpToolDefinition create(String name, String description) {
        if (name == null || name.isBlank()) {
            throw new McpToolNotFoundException("MCP tool name must not be blank");
        }
        return new McpToolDefinition(
                name.trim(), description != null ? description.trim() : "");
    }

    public void validate() {
        if (name.isBlank()) {
            throw new McpToolNotFoundException("MCP tool name must not be blank");
        }
    }
}
