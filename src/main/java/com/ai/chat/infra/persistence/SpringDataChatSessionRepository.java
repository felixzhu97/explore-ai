package com.ai.chat.infra.persistence;

import com.ai.chat.domain.model.ChatSession;
import com.ai.chat.domain.vo.ChatSessionId;
import com.ai.common.domain.vo.OwnerKey;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link ChatSession}. */
@Repository
public interface SpringDataChatSessionRepository extends JpaRepository<ChatSession, ChatSessionId> {

  /** Documentation. */
  List<ChatSession> findByOwnerKeyOrderByUpdatedAtDesc(OwnerKey ownerKey);

  /** Documentation. */
  Optional<ChatSession> findByIdAndOwnerKey(ChatSessionId id, OwnerKey ownerKey);

  /** Documentation. */
  List<ChatSession> findByUpdatedAtBeforeOrderByUpdatedAtAsc(Instant cutoff);
}
