package com.ai.mcp.service;

import org.springframework.ai.tool.ToolCallback;

/** Registry of Spring AI tool callbacks contributed by MCP servers. */
public interface McpToolCallbackRegistry {
  /** Registers the server's tool callbacks. */
  void registerToolCallbacks(ToolCallback[] tools, String serverName);

  /** Returns the registered tool callbacks. */
  ToolCallback[] getRegisteredToolCallbacks();
}
