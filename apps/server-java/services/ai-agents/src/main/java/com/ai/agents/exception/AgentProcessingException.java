package com.ai.agents.exception;

/**
 * Exception thrown when agent processing fails.
 */
public class AgentProcessingException extends RuntimeException {

    public AgentProcessingException(String message) {
        super(message);
    }

    public AgentProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
