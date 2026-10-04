package com.ai.mcp.controller.dto;

import java.util.List;

/**
 * MCP client status.
 *
 * @param connectedServers names of the connected MCP servers
 */
public record McpClientStatusResponse(
    McpClientStatus status, int registeredTools, List<String> connectedServers) {

  /** The client is ready as soon as the MCP module is enabled. */
  public enum McpClientStatus {
    READY
  }
}
