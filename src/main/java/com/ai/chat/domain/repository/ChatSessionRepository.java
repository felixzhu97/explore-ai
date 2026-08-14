package com.ai.chat.domain.repository;

import com.ai.chat.domain.model.ChatSession;
import com.ai.chat.domain.vo.ChatSessionId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Chat session repository interface. */
public interface ChatSessionRepository {
  /** Documentation. */
  Optional<ChatSession> findById(ChatSessionId id);

  /** Documentation. */
  Optional<ChatSession> findByIdAndClientId(ChatSessionId id, String clientId);

  /** Documentation. */
  void save(ChatSession session);

  /** Documentation. */
  void delete(ChatSessionId id);

  /** Documentation. */
  List<ChatSession> findByClientId(String clientId);

  /** Documentation. */
  List<ChatSession> findInactiveSince(Instant cutoff);

  /** Documentation. */
  boolean exists(ChatSessionId id);
}
