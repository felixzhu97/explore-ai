package com.ai.mcp.domain;

import com.ai.mcp.domain.McpSessionStatus;

public record McpServerConnection(String name, int toolCount, McpSessionStatus status) {

    public static McpServerConnection connected(String name, int toolCount) {
        return new McpServerConnection(name, toolCount, McpSessionStatus.ACTIVE);
    }

    public boolean isActive() {
        return status == McpSessionStatus.ACTIVE;
    }
}
