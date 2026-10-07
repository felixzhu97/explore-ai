package com.ai.mcp.domain.model;

import lombok.Value;

/** Connected MCP server with its tool count and session status. */
@Value
public class McpServerConnection {
  String name;
  int toolCount;
  McpSessionStatus status;

  /** Creates an active connection. */
  public static McpServerConnection createConnectedServer(String name, int toolCount) {
    return new McpServerConnection(name, toolCount, McpSessionStatus.ACTIVE);
  }

  /** Tells whether the connection is active. */
  public boolean isActive() {
    return status == McpSessionStatus.ACTIVE;
  }
}
