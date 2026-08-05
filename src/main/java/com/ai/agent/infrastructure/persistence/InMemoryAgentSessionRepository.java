package com.ai.agent.infrastructure.persistence;

import com.ai.agent.domain.model.AgentSession;
import com.ai.agent.domain.repository.AgentSessionRepository;
import com.ai.agent.domain.vo.SessionId;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryAgentSessionRepository implements AgentSessionRepository {

    private final Map<String, AgentSession> sessions = new ConcurrentHashMap<>();

    @Override
    public AgentSession save(AgentSession session) {
        sessions.put(session.id().value(), session);
        return session;
    }

    @Override
    public Optional<AgentSession> findById(SessionId id) {
        return Optional.ofNullable(sessions.get(id.value()));
    }

    @Override
    public List<AgentSession> findByClientId(String clientId) {
        return sessions.values().stream()
                .filter(s -> s.ownedBy(clientId))
                .sorted(Comparator.comparing(AgentSession::lastActivityAt).reversed())
                .toList();
    }

    @Override
    public void delete(SessionId id) {
        sessions.remove(id.value());
    }
}
