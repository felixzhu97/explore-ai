package com.ai.mcp.controller.dto;

import com.ai.mcp.domain.model.McpServerConnection;
import com.ai.mcp.domain.model.McpSessionStatus;

public record McpServerResponse(String name, int toolCount, McpSessionStatus status) {

  /** Maps a server connection to a response. */
  public static McpServerResponse createResponse(McpServerConnection connection) {
    return new McpServerResponse(
        connection.getName(), connection.getToolCount(), connection.getStatus());
  }
}
