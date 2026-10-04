package com.ai.chat.controller.dto;

import com.ai.chat.domain.model.ChatSession;
import java.time.Instant;

/** Session info DTO. */
public record SessionInfo(
    String sessionId, String title, int messageCount, Instant createdAt, Instant lastActivityAt) {
  /** Creates a summary of the session's id, title, message count, and timestamps. */
  public static SessionInfo from(ChatSession session) {
    return new SessionInfo(
        session.getId().toString(),
        session.getTitle(),
        session.getMessageCount(),
        session.getCreatedAt(),
        session.getLastActivityAt());
  }
}
