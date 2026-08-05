package com.ai.agent.domain.repository;

import com.ai.agent.domain.model.AgentSession;
import com.ai.agent.domain.vo.SessionId;

import java.util.List;
import java.util.Optional;

public interface AgentSessionRepository {

    AgentSession save(AgentSession session);

    Optional<AgentSession> findById(SessionId id);

    List<AgentSession> findByClientId(String clientId);

    void delete(SessionId id);
}
