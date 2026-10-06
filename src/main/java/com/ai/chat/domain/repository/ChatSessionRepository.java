package com.ai.chat.domain.repository;

import com.ai.chat.domain.model.ChatSession;
import com.ai.chat.domain.vo.ChatSessionId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Chat session repository interface. */
public interface ChatSessionRepository {
  /** Finds a session by id. */
  Optional<ChatSession> findById(ChatSessionId id);

  /** Finds the owner's session by id. */
  Optional<ChatSession> findByIdAndOwnerKey(ChatSessionId id, String ownerKey);

  /** Lists the owner's sessions. */
  List<ChatSession> findByOwnerKey(String ownerKey);

  /** Lists sessions with no activity since the cutoff. */
  List<ChatSession> findInactiveSince(Instant cutoff);

  /** Tells whether a session with the id exists. */
  boolean exists(ChatSessionId id);

  /** Saves the session. */
  void save(ChatSession session);

  /** Deletes the session. */
  void delete(ChatSessionId id);
}
