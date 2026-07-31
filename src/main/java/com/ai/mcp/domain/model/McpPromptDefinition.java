package com.ai.mcp.domain.model;

public record McpPromptDefinition(String name, String description, String serverName) {

    public McpPromptDefinition {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("MCP prompt name must not be blank");
        }
        name = name.trim();
        description = description != null ? description.trim() : "";
        if (serverName == null || serverName.isBlank()) {
            throw new IllegalArgumentException("MCP prompt serverName must not be blank");
        }
        serverName = serverName.trim();
    }

    public static McpPromptDefinition create(String name, String description, String serverName) {
        return new McpPromptDefinition(name, description, serverName);
    }
}
