package com.ai.mcp.domain.model;

public record McpResourceDefinition(String uri, String name, String description, String serverName) {

    public McpResourceDefinition {
        if (uri == null || uri.isBlank()) {
            throw new IllegalArgumentException("MCP resource uri must not be blank");
        }
        uri = uri.trim();
        name = name != null ? name.trim() : "";
        description = description != null ? description.trim() : "";
        if (serverName == null || serverName.isBlank()) {
            throw new IllegalArgumentException("MCP resource serverName must not be blank");
        }
        serverName = serverName.trim();
    }

    public static McpResourceDefinition create(String uri, String name, String description, String serverName) {
        return new McpResourceDefinition(uri, name, description, serverName);
    }
}
