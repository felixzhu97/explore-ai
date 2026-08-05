package com.ai.agent.domain.model;

import com.ai.agent.domain.vo.SessionId;

import java.time.Instant;
import java.util.Objects;

public class AgentSession {

    private final SessionId id;
    private final String clientId;
    private String title;
    private final Instant createdAt;
    private Instant lastActivityAt;

    private AgentSession(SessionId id, String clientId, String title, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.clientId = requireClientId(clientId);
        this.title = title == null || title.isBlank() ? "New Agent" : title.trim();
        this.createdAt = Objects.requireNonNull(createdAt);
        this.lastActivityAt = createdAt;
    }

    private static String requireClientId(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId must not be blank");
        }
        return clientId.trim();
    }

    public static AgentSession create(String title, String clientId) {
        return new AgentSession(SessionId.generate(), clientId, title, Instant.now());
    }

    public static AgentSession createWithId(SessionId id, String title, String clientId) {
        return new AgentSession(id, clientId, title, Instant.now());
    }

    public SessionId id() {
        return id;
    }

    public String clientId() {
        return clientId;
    }

    public String title() {
        return title;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant lastActivityAt() {
        return lastActivityAt;
    }

    public boolean ownedBy(String clientId) {
        return this.clientId.equals(clientId);
    }

    public void touch() {
        this.lastActivityAt = Instant.now();
    }

    public void rename(String title) {
        if (title != null && !title.isBlank()) {
            this.title = title.trim();
        }
    }
}
