package com.ai.mcp.controller.dto;

import com.ai.mcp.domain.model.McpSessionStatus;
import com.ai.mcp.domain.vo.McpServerConnection;

public record McpServerResponse(String name, int toolCount, McpSessionStatus status) {

  /** Maps a server connection to a response. */
  public static McpServerResponse from(McpServerConnection connection) {
    return new McpServerResponse(connection.name(), connection.toolCount(), connection.status());
  }
}
