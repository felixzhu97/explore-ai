package com.ai.agents.domain.agent;

/**
 * Status of an AI Agent.
 */
public enum AgentStatus {
    /**
     * Agent is online and ready to process requests.
     */
    ONLINE,

    /**
     * Agent is busy processing a request.
     */
    BUSY,

    /**
     * Agent is temporarily unavailable.
     */
    UNAVAILABLE,

    /**
     * Agent is offline/uninitialized.
     */
    OFFLINE
}
