package com.ai.ai.domain.exception;

/**
 * Domain exception indicating chat session was not found.
 */
public class ChatSessionNotFoundException extends RuntimeException {

    private final String sessionId;

    public ChatSessionNotFoundException(String sessionId) {
        super("Chat session not found: " + sessionId);
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }
}
