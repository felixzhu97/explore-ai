package com.ai.mcp.domain.model;

import com.ai.mcp.domain.exception.InvalidMcpSessionException;

import java.util.Objects;
import java.util.UUID;

public class McpSession {

    private final UUID id;
    private final String serverName;
    private final int toolCount;
    private McpSessionStatus status;

    private McpSession(UUID id, String serverName, int toolCount) {
        this.id = Objects.requireNonNull(id);
        this.serverName = validateServerName(serverName);
        this.toolCount = Math.max(toolCount, 0);
        this.status = McpSessionStatus.ACTIVE;
    }

    public static McpSession open(String serverName, int toolCount) {
        return new McpSession(UUID.randomUUID(), serverName, toolCount);
    }

    public static McpSession reconstitute(
            UUID id, String serverName, int toolCount, McpSessionStatus status) {
        McpSession session = new McpSession(id, serverName, toolCount);
        session.status = status;
        return session;
    }

    public void activate() {
        ensureNotClosed();
        status = McpSessionStatus.ACTIVE;
    }

    public void close() {
        if (status == McpSessionStatus.CLOSED) {
            throw new InvalidMcpSessionException("Session already closed: " + id);
        }
        status = McpSessionStatus.CLOSED;
    }

    public boolean isActive() {
        return status == McpSessionStatus.ACTIVE;
    }

    public UUID id() {
        return id;
    }

    public String serverName() {
        return serverName;
    }

    public int toolCount() {
        return toolCount;
    }

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
