package com.ai.mcp.application.port;

import org.springframework.ai.tool.ToolCallback;

public interface McpToolCallbackRegistry {

    void registerToolCallbacks(ToolCallback[] tools, String serverName);

    ToolCallback[] getRegisteredToolCallbacks();
}
