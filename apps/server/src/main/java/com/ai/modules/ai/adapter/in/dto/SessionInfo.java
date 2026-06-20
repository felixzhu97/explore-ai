package com.ai.adapter.in.dto;

import com.ai.domain.model.ChatSession;

import java.time.Instant;

/**
 * Session info DTO.
 */
public record SessionInfo(
    String sessionId,
    String title,
    int messageCount,
    Instant createdAt,
    Instant lastActivityAt
) {
    public static SessionInfo from(ChatSession session) {
        return new SessionInfo(
            session.getId().toString(),
            session.getTitle(),
            session.getMessageCount(),
            session.getCreatedAt(),
            session.getLastActivityAt()
        );
    }
}
