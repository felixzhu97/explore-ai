package com.ai.chat.domain.repository;

import com.ai.chat.domain.model.ChatSession;
import com.ai.chat.domain.vo.ChatSessionId;

import java.util.List;
import java.util.Optional;

/**
 * Chat session repository interface.
 */
public interface ChatSessionRepository {

    Optional<ChatSession> findById(ChatSessionId id);

    void save(ChatSession session);

    void delete(ChatSessionId id);

    List<ChatSession> findAll();

    boolean exists(ChatSessionId id);
}
