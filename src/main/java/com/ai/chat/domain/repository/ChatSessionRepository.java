package com.ai.chat.domain.repository;

import com.ai.chat.domain.model.ChatSession;
import com.ai.chat.domain.model.ChatSessionId;
import com.ai.common.domain.model.OwnerKey;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Repository of chat session metadata; the messages live in chat memory. */
public interface ChatSessionRepository extends Repository<ChatSession, ChatSessionId> {

  /** Finds a session by id regardless of owner, for background work on a known session. */
  Optional<ChatSession> findById(ChatSessionId id);

  /** Finds the owner's session by id. */
  Optional<ChatSession> findByIdAndOwnerKey(ChatSessionId id, OwnerKey ownerKey);

  /** Lists the owner's sessions, most recently active first. */
  List<ChatSession> findAllByOwnerKeyOrderByLastActivityAtDesc(OwnerKey ownerKey);

  /** Lists every owner's sessions with no activity since the cutoff, oldest first. */
  List<ChatSession> findAllByLastActivityAtBeforeOrderByLastActivityAtAsc(Instant cutoff);

  /** Tells whether any owner has a session with the id. */
  boolean existsById(ChatSessionId id);

  /** Saves the session. */
  ChatSession save(ChatSession session);

  /** Deletes the session. */
  @Transactional
  void deleteById(ChatSessionId id);
}
