package com.ai.mcp.domain.vo;

import com.ai.mcp.domain.model.McpSessionStatus;

public record McpServerConnection(
        String name,
        int toolCount,
        int resourceCount,
        int promptCount,
        boolean toolsSupported,
        boolean resourcesSupported,
        boolean promptsSupported,
        McpSessionStatus status) {

    public static McpServerConnection connected(
            String name,
            int toolCount,
            int resourceCount,
            int promptCount,
            boolean toolsSupported,
            boolean resourcesSupported,
            boolean promptsSupported) {
        return new McpServerConnection(
                name,
                toolCount,
                resourceCount,
                promptCount,
                toolsSupported,
                resourcesSupported,
                promptsSupported,
                McpSessionStatus.ACTIVE);
    }

    public static McpServerConnection connected(String name, int toolCount) {
        return connected(name, toolCount, 0, 0, toolCount > 0, false, false);
    }

    public boolean isActive() {
        return status == McpSessionStatus.ACTIVE;
    }
}
