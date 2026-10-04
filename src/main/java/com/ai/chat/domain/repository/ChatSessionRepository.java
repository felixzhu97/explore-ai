package com.ai.chat.domain.repository;

import com.ai.chat.domain.model.ChatSession;
import com.ai.chat.domain.vo.ChatSessionId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Chat session repository interface. */
public interface ChatSessionRepository {
  Optional<ChatSession> findById(ChatSessionId id);

  Optional<ChatSession> findByIdAndOwnerKey(ChatSessionId id, String ownerKey);

  void save(ChatSession session);

  void delete(ChatSessionId id);

  List<ChatSession> findByOwnerKey(String ownerKey);

  List<ChatSession> findInactiveSince(Instant cutoff);

  boolean exists(ChatSessionId id);
}
