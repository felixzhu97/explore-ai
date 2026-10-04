package com.ai.mcp.service;

import org.springframework.ai.tool.ToolCallback;

/** Registry of Spring AI tool callbacks contributed by MCP servers. */
public interface McpToolCallbackRegistry {
  void registerToolCallbacks(ToolCallback[] tools, String serverName);

  ToolCallback[] getRegisteredToolCallbacks();
}
