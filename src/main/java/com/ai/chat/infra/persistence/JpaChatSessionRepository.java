package com.ai.chat.infra.persistence;

import com.ai.chat.domain.model.ChatSession;
import com.ai.chat.domain.repository.ChatSessionRepository;
import com.ai.chat.domain.vo.ChatSessionId;
import com.ai.common.domain.vo.OwnerKey;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** JPA adapter for chat session metadata (messages stored in ChatMemory). */
@Repository
public class JpaChatSessionRepository implements ChatSessionRepository {

  private final SpringDataChatSessionRepository delegate;

  /** Documentation. */
  public JpaChatSessionRepository(SpringDataChatSessionRepository delegate) {
    this.delegate = delegate;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<ChatSession> findById(ChatSessionId id) {
    return delegate.findById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<ChatSession> findByIdAndClientId(ChatSessionId id, String clientId) {
    return delegate.findByIdAndOwnerKeyValue(id.value(), toOwnerKeyValue(clientId));
  }

  @Override
  @Transactional
  public void save(ChatSession session) {
    delegate.saveAndFlush(session);
  }

  @Override
  @Transactional
  public void delete(ChatSessionId id) {
    delegate.deleteById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ChatSession> findByClientId(String clientId) {
    return delegate.findByOwnerKeyValueOrderByUpdatedAtDesc(toOwnerKeyValue(clientId));
  }

  @Override
  @Transactional(readOnly = true)
  public List<ChatSession> findInactiveSince(Instant cutoff) {
    return delegate.findByUpdatedAtBeforeOrderByUpdatedAtAsc(cutoff);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean exists(ChatSessionId id) {
    return delegate.existsById(id);
  }

  private static String toOwnerKeyValue(String clientId) {
    if (clientId == null || clientId.isBlank()) {
      throw new IllegalArgumentException("clientId is required");
    }
    String trimmed = clientId.trim();
    if (trimmed.startsWith(OwnerKey.CLIENT_PREFIX) || trimmed.startsWith(OwnerKey.ACCOUNT_PREFIX)) {
      return trimmed;
    }
    return OwnerKey.forClient(trimmed).value();
  }
}
