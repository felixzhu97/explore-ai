package com.ai.mcp.domain.model;

public record McpToolDefinition(String name, String description) {

    public McpToolDefinition {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("MCP tool name must not be blank");
        }
        name = name.trim();
        description = description != null ? description.trim() : "";
    }

    public static McpToolDefinition create(String name, String description) {
        return new McpToolDefinition(name, description);
    }
}
