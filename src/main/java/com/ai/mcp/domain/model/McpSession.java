package com.ai.mcp.domain.model;

import com.ai.mcp.domain.exception.InvalidMcpSessionException;
import java.util.Objects;
import java.util.UUID;

/** Connection session to an MCP server, tracking its tool count and active or closed status. */
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

  /** Restores a stored session. */
  public static McpSession reconstitute(
      UUID id, String serverName, int toolCount, McpSessionStatus status) {
    McpSession session = new McpSession(id, serverName, toolCount);
    session.status = status;
    return session;
  }

  /** Marks the session active. */
  public void activate() {
    ensureNotClosed();
    status = McpSessionStatus.ACTIVE;
  }

  /** Marks the session closed, rejecting a second close. */
  public void close() {
    if (status == McpSessionStatus.CLOSED) {
      throw new InvalidMcpSessionException("Session already closed: " + id);
    }
    status = McpSessionStatus.CLOSED;
  }

  /** Tells whether the session is active. */
  public boolean isActive() {
    return status == McpSessionStatus.ACTIVE;
  }

  /** Returns the session id. */
  public UUID id() {
    return id;
  }

  /** Returns the server name. */
  public String serverName() {
    return serverName;
  }

  /** Returns the number of tools. */
  public int toolCount() {
    return toolCount;
  }

  /** Returns the session status. */
  public McpSessionStatus status() {
    return status;
  }

  private void ensureNotClosed() {
    if (status == McpSessionStatus.CLOSED) {
      throw new InvalidMcpSessionException("Cannot activate closed session: " + id);
    }
  }

  private static String validateServerName(String serverName) {
    if (serverName == null || serverName.isBlank()) {
      throw new InvalidMcpSessionException("Server name must not be blank");
    }
    return serverName.trim();
  }
}
