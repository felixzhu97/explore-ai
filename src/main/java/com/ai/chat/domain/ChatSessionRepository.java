package com.ai.chat.domain;

import com.ai.chat.domain.ChatSession;
import com.ai.chat.domain.ChatSessionId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Chat session repository interface.
 */
public interface ChatSessionRepository {

    Optional<ChatSession> findById(ChatSessionId id);

    Optional<ChatSession> findByIdAndClientId(ChatSessionId id, String clientId);

    void save(ChatSession session);

    void delete(ChatSessionId id);

    List<ChatSession> findByClientId(String clientId);

    List<ChatSession> findInactiveSince(Instant cutoff);

    boolean exists(ChatSessionId id);
}
