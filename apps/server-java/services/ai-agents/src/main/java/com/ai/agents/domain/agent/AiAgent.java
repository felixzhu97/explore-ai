package com.ai.agents.domain.agent;

import com.ai.agents.domain.model.AgentRequest;
import com.ai.agents.domain.model.AgentResponse;

/**
 * Core interface for all AI Agents in the system.
 * Each agent processes requests and returns responses using LangChain4j.
 */
public interface AiAgent {

    /**
     * Get the unique identifier for this agent.
     */
    String getAgentId();

    /**
     * Get the display name for this agent.
     */
    String getName();

    /**
     * Get a description of what this agent does.
     */
    String getDescription();

    /**
     * Check if this agent can handle the given request.
     *
     * @param request The agent request to check
     * @return true if this agent can handle the request
     */
    boolean canHandle(AgentRequest request);

    /**
     * Process the given request and return a response.
     *
     * @param request The agent request to process
     * @return The agent response
     */
    AgentResponse process(AgentRequest request);

    /**
     * Get the agent type identifier.
     */
    AgentType getAgentType();

    /**
     * Get the status of this agent.
     */
    default AgentStatus getStatus() {
        return AgentStatus.ONLINE;
    }
}
