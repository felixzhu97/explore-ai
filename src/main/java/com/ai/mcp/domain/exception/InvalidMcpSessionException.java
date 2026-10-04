package com.ai.mcp.domain.exception;

/** Thrown when an MCP session has a blank server name or an invalid status transition. */
public class InvalidMcpSessionException extends RuntimeException {
  public InvalidMcpSessionException(String message) {
    super(message);
  }
}
