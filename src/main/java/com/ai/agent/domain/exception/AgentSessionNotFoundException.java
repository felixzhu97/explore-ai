package com.ai.agent.domain.exception;

import com.ai.agent.domain.vo.SessionId;

public class AgentSessionNotFoundException extends RuntimeException {

    private final SessionId sessionId;

    public AgentSessionNotFoundException(SessionId sessionId) {
        super("Agent session not found: " + sessionId.value());
        this.sessionId = sessionId;
    }

    public SessionId sessionId() {
        return sessionId;
    }
}
