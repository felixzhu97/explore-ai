package com.ai.mcp.domain.model;

import com.ai.common.exception.DomainException;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

/** Connection session to an MCP server, tracking its tool count and active or closed status. */
@Getter
public class McpSession {

  private final UUID id;
  private final String serverName;
  private final int toolCount;
  private volatile McpSessionStatus status;

  private McpSession(UUID id, String serverName, int toolCount) {
    this.id = Objects.requireNonNull(id);
    this.serverName = validateServerName(serverName);
    this.toolCount = Math.max(toolCount, 0);
    this.status = McpSessionStatus.ACTIVE;
  }

  /** Opens a new session for the server. */
  public static McpSession open(String serverName, int toolCount) {
    return new McpSession(UUID.randomUUID(), serverName, toolCount);
  }

  /** Marks the session closed, rejecting a second close. */
  public void close() {
    if (status == McpSessionStatus.CLOSED) {
      throw DomainException.createInvalidError(
          "INVALID_MCP_SESSION", "Session already closed: " + id);
    }
    status = McpSessionStatus.CLOSED;
  }

  /** Tells whether the session is active. */
  public boolean isActive() {
    return status == McpSessionStatus.ACTIVE;
  }

  private static String validateServerName(String serverName) {
    if (serverName == null || serverName.isBlank()) {
      throw DomainException.createInvalidError(
          "INVALID_MCP_SESSION", "Server name must not be blank");
    }
    return serverName.trim();
  }
}
