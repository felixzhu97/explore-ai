package com.ai.chat.controller.dto;

import com.ai.chat.domain.model.ChatSession;
import java.time.Instant;

/** Session info DTO. */
public record SessionResponse(
    String sessionId, String title, int messageCount, Instant createdAt, Instant lastActivityAt) {
  /** Creates a summary of the session's id, title, message count, and timestamps. */
  public static SessionResponse from(ChatSession session) {
    return new SessionResponse(
        session.getId().toString(),
        session.getTitle(),
        session.getMessageCount(),
        session.getCreatedAt(),
        session.getLastActivityAt());
  }
}
