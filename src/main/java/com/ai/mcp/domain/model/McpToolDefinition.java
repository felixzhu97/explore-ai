package com.ai.mcp.domain.model;

public record McpToolDefinition(String name, String description, String serverName) {

    public McpToolDefinition {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("MCP tool name must not be blank");
        }
        name = name.trim();
        description = description != null ? description.trim() : "";
        if (serverName == null || serverName.isBlank()) {
            throw new IllegalArgumentException("MCP tool serverName must not be blank");
        }
        serverName = serverName.trim();
    }

    public static McpToolDefinition create(String name, String description, String serverName) {
        return new McpToolDefinition(name, description, serverName);
    }
}
