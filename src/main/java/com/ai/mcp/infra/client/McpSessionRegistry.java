package com.ai.mcp.infra.client;

import com.ai.mcp.domain.model.McpSession;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Thread-safe in-memory store of MCP sessions keyed by session id. */
public class McpSessionRegistry {

  private final Map<UUID, McpSession> sessions = new ConcurrentHashMap<>();

  /** Opens and stores a session for the server. */
  public McpSession registerSession(String serverName, int toolCount) {
    McpSession session = McpSession.open(serverName, toolCount);
    sessions.put(session.getId(), session);
    return session;
  }

  /** Finds a session by server name. */
  public Optional<McpSession> findByServerName(String serverName) {
    return sessions.values().stream()
        .filter(session -> session.getServerName().equals(serverName))
        .findFirst();
  }

  /** Finds an active session by server name. */
  public Optional<McpSession> findActiveByServerName(String serverName) {
    return sessions.values().stream()
        .filter(session -> session.getServerName().equals(serverName) && session.isActive())
        .findFirst();
  }

  /** Closes the session with the given id if it is known. */
  public void closeSession(UUID sessionId) {
    McpSession session = sessions.get(sessionId);
    if (session != null) {
      session.close();
    }
  }

  /** Lists the active sessions. */
  public List<McpSession> listActiveSessions() {
    return sessions.values().stream().filter(McpSession::isActive).toList();
  }

  /** Counts the active sessions. */
  public int countActiveSessions() {
    return (int) sessions.values().stream().filter(McpSession::isActive).count();
  }

  /** Removes all sessions. */
  public void clear() {
    sessions.clear();
  }
}
