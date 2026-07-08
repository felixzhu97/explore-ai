package com.ai.mcp.domain.service;

import com.ai.mcp.domain.model.McpSession;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class McpSessionManager {

    private final Map<UUID, McpSession> sessions = new ConcurrentHashMap<>();

    public McpSession registerSession(String serverName, int toolCount) {
        McpSession session = McpSession.open(serverName, toolCount);
        sessions.put(session.id(), session);
        return session;
    }

    public Optional<McpSession> findByServerName(String serverName) {
        return sessions.values().stream()
                .filter(session -> session.serverName().equals(serverName))
                .findFirst();
    }

    public Optional<McpSession> findActiveByServerName(String serverName) {
        return sessions.values().stream()
                .filter(session -> session.serverName().equals(serverName) && session.isActive())
                .findFirst();
    }

    public void closeSession(UUID sessionId) {
        McpSession session = sessions.get(sessionId);
        if (session != null) {
            session.close();
        }
    }

    public List<McpSession> activeSessions() {
        return sessions.values().stream().filter(McpSession::isActive).toList();
    }

    public int activeSessionCount() {
        return (int) sessions.values().stream()
                .filter(McpSession::isActive)
                .count();
    }

    public void clear() {
        sessions.clear();
    }
}
