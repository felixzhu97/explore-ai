package com.ai.mcp.domain.vo;

import com.ai.mcp.domain.model.McpSessionStatus;

public record McpServerConnection(String name, int toolCount, McpSessionStatus status) {
  /** Creates an active connection. */
  public static McpServerConnection connected(String name, int toolCount) {
    return new McpServerConnection(name, toolCount, McpSessionStatus.ACTIVE);
  }

  /** Tells whether the connection is active. */
  public boolean isActive() {
    return status == McpSessionStatus.ACTIVE;
  }
}
