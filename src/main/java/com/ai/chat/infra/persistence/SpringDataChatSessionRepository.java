package com.ai.chat.infra.persistence;

import com.ai.chat.domain.model.ChatSession;
import com.ai.chat.domain.vo.ChatSessionId;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link ChatSession}. */
@Repository
public interface SpringDataChatSessionRepository extends JpaRepository<ChatSession, ChatSessionId> {

  List<ChatSession> findByUpdatedAtBeforeOrderByUpdatedAtAsc(Instant cutoff);
}
