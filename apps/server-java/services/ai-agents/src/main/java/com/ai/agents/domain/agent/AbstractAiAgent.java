package com.ai.agents.domain.agent;

import com.ai.agents.domain.model.AgentRequest;
import com.ai.agents.domain.model.AgentResponse;
import com.ai.agents.domain.model.ToolResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for all AI Agents.
 * Provides common functionality for reasoning and tool usage.
 */
public abstract class AbstractAiAgent implements AiAgent {

    protected final String agentId;
    protected final String name;
    protected final String description;
    protected final AgentType agentType;
    protected AgentStatus status = AgentStatus.ONLINE;

    protected AbstractAiAgent(
            String agentId,
            String name,
            String description,
            AgentType agentType
    ) {
        this.agentId = agentId;
        this.name = name;
        this.description = description;
        this.agentType = agentType;
    }

    @Override
    public String getAgentId() {
        return agentId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public AgentType getAgentType() {
        return agentType;
    }

    @Override
    public AgentStatus getStatus() {
        return status;
    }

    protected void setStatus(AgentStatus status) {
        this.status = status;
    }

    @Override
    public boolean canHandle(AgentRequest request) {
        return true;
    }

    /**
     * Get the system prompt for this agent.
     */
    protected abstract String getSystemPrompt();

    /**
     * Process the request after optional tool execution.
     */
    protected abstract String processWithContext(
            AgentRequest request,
            List<ToolResult> toolResults
    );

    @Override
    public AgentResponse process(AgentRequest request) {
        try {
            setStatus(AgentStatus.BUSY);
            return doProcess(request);
        } finally {
            setStatus(AgentStatus.ONLINE);
        }
    }

    protected AgentResponse doProcess(AgentRequest request) {
        List<ToolResult> toolResults = new ArrayList<>();

        String response = processWithContext(request, toolResults);

        return AgentResponse.success(response, agentId, agentType.getId());
    }
}
