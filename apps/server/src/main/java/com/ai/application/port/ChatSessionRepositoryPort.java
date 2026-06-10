package com.ai.application.port;

import com.ai.domain.model.ChatSession;
import com.ai.domain.vo.ChatSessionId;

import java.util.List;
import java.util.Optional;

/**
 * Repository port interface - defines application layer requirements for repository.
 * Dependency inversion: application layer defines interface, infrastructure layer implements.
 */
public interface ChatSessionRepositoryPort {

    /**
     * Finds a session by ID.
     */
    Optional<ChatSession> findById(ChatSessionId id);

    /**
     * Saves or updates a session.
     */
    void save(ChatSession session);

    /**
     * Deletes a session.
     */
    void delete(ChatSessionId id);

    /**
     * Finds all sessions.
     */
    List<ChatSession> findAll();

    /**
     * Checks if a session exists.
     */
    boolean exists(ChatSessionId id);

    /**
     * Gets or creates the default session.
     */
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
