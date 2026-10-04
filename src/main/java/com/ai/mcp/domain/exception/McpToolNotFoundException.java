package com.ai.mcp.domain.exception;

/** Thrown when a requested MCP tool is not registered. */
public class McpToolNotFoundException extends RuntimeException {
  public McpToolNotFoundException(String message) {
    super(message);
  }
}
