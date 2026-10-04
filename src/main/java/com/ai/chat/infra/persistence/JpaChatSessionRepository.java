package com.ai.chat.infra.persistence;

import com.ai.chat.domain.model.ChatSession;
import com.ai.chat.domain.repository.ChatSessionRepository;
import com.ai.chat.domain.vo.ChatSessionId;
import com.ai.common.domain.vo.OwnerKey;
import com.ai.common.infra.persistence.OwnerPartitionScope;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** JPA adapter for chat session metadata (messages stored in ChatMemory). */
@Repository
public class JpaChatSessionRepository implements ChatSessionRepository {

  private final SpringDataChatSessionRepository delegate;
  private static final Sort MOST_RECENT_FIRST = Sort.by(Sort.Direction.DESC, "updatedAt");

  private final OwnerPartitionScope ownerPartition;

  public JpaChatSessionRepository(
      SpringDataChatSessionRepository delegate, OwnerPartitionScope ownerPartition) {
    this.delegate = delegate;
    this.ownerPartition = ownerPartition;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<ChatSession> findById(ChatSessionId id) {
    return delegate.findById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<ChatSession> findByIdAndOwnerKey(ChatSessionId id, String ownerKey) {
    return ownerPartition.findOne(toOwnerKey(ownerKey), () -> delegate.findById(id));
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
  public List<ChatSession> findByOwnerKey(String ownerKey) {
    return ownerPartition.apply(toOwnerKey(ownerKey), () -> delegate.findAll(MOST_RECENT_FIRST));
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

  private static OwnerKey toOwnerKey(String ownerKey) {
    if (ownerKey == null || ownerKey.isBlank()) {
      throw new IllegalArgumentException("ownerKey is required");
    }
    String trimmed = ownerKey.trim();
    if (trimmed.startsWith(OwnerKey.CLIENT_PREFIX) || trimmed.startsWith(OwnerKey.ACCOUNT_PREFIX)) {
      return OwnerKey.parse(trimmed);
    }
    return OwnerKey.forClient(trimmed);
  }
}
