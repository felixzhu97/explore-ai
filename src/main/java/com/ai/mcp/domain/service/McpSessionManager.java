package com.ai.mcp.domain.service;

import com.ai.mcp.domain.model.McpSession;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Documentation. */
public class McpSessionManager {

  private final Map<UUID, McpSession> sessions = new ConcurrentHashMap<>();

  /** Documentation. */
  public McpSession registerSession(String serverName, int toolCount) {
    McpSession session = McpSession.open(serverName, toolCount);
    sessions.put(session.id(), session);
    return session;
  }

  /** Documentation. */
  public Optional<McpSession> findByServerName(String serverName) {
    return sessions.values().stream()
        .filter(session -> session.serverName().equals(serverName))
        .findFirst();
  }

  /** Documentation. */
  public Optional<McpSession> findActiveByServerName(String serverName) {
    return sessions.values().stream()
        .filter(session -> session.serverName().equals(serverName) && session.isActive())
        .findFirst();
  }

  /** Documentation. */
  public void closeSession(UUID sessionId) {
    McpSession session = sessions.get(sessionId);
    if (session != null) {
      session.close();
    }
  }

  /** Documentation. */
  public List<McpSession> activeSessions() {
    return sessions.values().stream().filter(McpSession::isActive).toList();
  }

  /** Documentation. */
  public int activeSessionCount() {
    return (int) sessions.values().stream().filter(McpSession::isActive).count();
  }

  /** Documentation. */
  public void clear() {
    sessions.clear();
  }
}
