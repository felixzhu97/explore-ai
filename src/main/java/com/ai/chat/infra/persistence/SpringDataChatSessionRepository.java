package com.ai.chat.infra.persistence;

import com.ai.chat.domain.model.ChatSession;
import com.ai.chat.domain.vo.ChatSessionId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link ChatSession}. */
@Repository
public interface SpringDataChatSessionRepository extends JpaRepository<ChatSession, ChatSessionId> {

  /** Documentation. */
  @Query(
      value =
          """
          SELECT * FROM chat_sessions
          WHERE owner_key = :ownerKeyValue
          ORDER BY last_activity_at DESC
          """,
      nativeQuery = true)
  List<ChatSession> findByOwnerKeyValueOrderByUpdatedAtDesc(
      @Param("ownerKeyValue") String ownerKeyValue);

  /** Documentation. */
  @Query(
      value =
          """
          SELECT * FROM chat_sessions
          WHERE id = :id AND owner_key = :ownerKeyValue
          """,
      nativeQuery = true)
  Optional<ChatSession> findByIdAndOwnerKeyValue(
      @Param("id") String id, @Param("ownerKeyValue") String ownerKeyValue);

  /** Documentation. */
  List<ChatSession> findByUpdatedAtBeforeOrderByUpdatedAtAsc(Instant cutoff);
}
