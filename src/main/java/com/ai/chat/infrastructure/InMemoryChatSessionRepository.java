package com.ai.chat.infrastructure;

import com.ai.chat.domain.ChatSessionRepository;
import com.ai.chat.domain.ChatSession;
import com.ai.chat.domain.ChatSessionId;

import java.time.Instant;
import java.util.Comparator;
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
    public Optional<ChatSession> findByIdAndClientId(ChatSessionId id, String clientId) {
        return findById(id).filter(session -> session.belongsTo(clientId));
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
    public List<ChatSession> findByClientId(String clientId) {
        return storage.values().stream()
                .filter(session -> session.belongsTo(clientId))
                .sorted(Comparator.comparing(ChatSession::getLastActivityAt).reversed())
                .toList();
    }

    @Override
    public List<ChatSession> findInactiveSince(Instant cutoff) {
        return storage.values().stream()
                .filter(session -> session.getLastActivityAt().isBefore(cutoff))
                .sorted(Comparator.comparing(ChatSession::getLastActivityAt))
                .toList();
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
