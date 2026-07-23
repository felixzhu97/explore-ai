package com.ai.agent.infrastructure.prompt;

import com.ai.agent.domain.model.AgentDefinition;
import com.ai.agent.domain.vo.AgentType;
import com.ai.common.infrastructure.prompt.PromptTemplates;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AgentPromptCatalog {

    private final PromptTemplates promptTemplates;

    public AgentPromptCatalog(PromptTemplates promptTemplates) {
        this.promptTemplates = promptTemplates;
    }

    public List<AgentDefinition> defaultAgents() {
        return List.of(
                AgentDefinition.create(
                        AgentType.supervisor(),
                        "Supervisor",
                        "Multi-agent orchestrator - coordinates specialized agents for complex tasks",
                        promptTemplates.loadAgentSystemPrompt("supervisor")),
                AgentDefinition.create(
                        AgentType.of("research"),
                        "Research Agent",
                        "Live web research via search tools",
                        promptTemplates.loadAgentSystemPrompt("research")),
                AgentDefinition.create(
                        AgentType.of("weather"),
                        "Weather Agent",
                        "Current weather and forecasts via weather tools",
                        promptTemplates.loadAgentSystemPrompt("weather")),
                AgentDefinition.create(
                        AgentType.of("vectordb"),
                        "Knowledge Agent",
                        "Document retrieval and knowledge-base Q&A via search tools",
                        promptTemplates.loadAgentSystemPrompt("vectordb")),
                AgentDefinition.create(
                        AgentType.of("analyst"),
                        "Analyst Agent",
                        "Synthesizes prior worker outputs into a clear brief",
                        promptTemplates.loadAgentSystemPrompt("analyst")));
    }
}
