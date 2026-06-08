package com.ai.agents.exception;

/**
 * Exception thrown when an agent is not found.
 */
public class AgentNotFoundException extends RuntimeException {

    public AgentNotFoundException(String agentId) {
        super("Agent not found: " + agentId);
    }

    public AgentNotFoundException(String agentId, Throwable cause) {
        super("Agent not found: " + agentId, cause);
    }
}
