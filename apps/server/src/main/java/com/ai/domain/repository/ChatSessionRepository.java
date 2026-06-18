package com.ai.domain.repository;

import com.ai.domain.model.ChatSession;
import com.ai.domain.vo.ChatSessionId;

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

    default ChatSession getOrCreateDefaultSession() {
        List<ChatSession> sessions = findAll();
        if (sessions.isEmpty()) {
            ChatSession newSession = ChatSession.create("Default Chat");
            save(newSession);
            return newSession;
        }
        return sessions.get(0);
    }
}
