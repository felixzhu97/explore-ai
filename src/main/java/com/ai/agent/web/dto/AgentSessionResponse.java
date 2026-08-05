package com.ai.agent.web.dto;

import com.ai.agent.domain.model.AgentSession;

import java.time.Instant;

public record AgentSessionResponse(
        String id,
        String title,
        Instant createdAt,
        Instant lastActivityAt) {

    public static AgentSessionResponse from(AgentSession session) {
        return new AgentSessionResponse(
                session.id().value(),
                session.title(),
                session.createdAt(),
                session.lastActivityAt());
    }
}
