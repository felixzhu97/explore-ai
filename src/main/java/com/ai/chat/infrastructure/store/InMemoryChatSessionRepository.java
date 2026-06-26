package com.ai.chat.infrastructure.store;

import com.ai.chat.domain.repository.ChatSessionRepository;
import com.ai.chat.domain.model.ChatSession;
import com.ai.chat.domain.vo.ChatSessionId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory session repository implementation.
 * Simple in-memory storage for development and testing environments.
 */
public class InMemoryChatSessionRepository implements ChatSessionRepository {

    private final Map<ChatSessionId, ChatSession> storage = new ConcurrentHashMap<>();

    @Override
    public Optional<ChatSession> findById(ChatSessionId id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public void save(ChatSession session) {
        storage.put(session.getId(), session);
    }

    @Override
    public void delete(ChatSessionId id) {
        storage.remove(id);
    }

    @Override
    public List<ChatSession> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public boolean exists(ChatSessionId id) {
        return storage.containsKey(id);
    }

    /**
     * Clears all sessions. Primarily used for testing.
     */
    public void clear() {
        storage.clear();
    }

    public int size() {
        return storage.size();
    }
}
