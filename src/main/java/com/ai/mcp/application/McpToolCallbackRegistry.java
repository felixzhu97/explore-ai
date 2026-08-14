package com.ai.mcp.application;

import org.springframework.ai.tool.ToolCallback;

/** Documentation. */
public interface McpToolCallbackRegistry {
  /** Documentation. */
  void registerToolCallbacks(ToolCallback[] tools, String serverName);

  /** Documentation. */
  ToolCallback[] getRegisteredToolCallbacks();
}
