package com.ai.agent.infrastructure.prompt;

import com.ai.agent.domain.model.AgentDefinition;
import com.ai.agent.domain.vo.AgentType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AgentPromptCatalog {

    public List<AgentDefinition> defaultAgents() {
        return List.of(
                AgentDefinition.create(
                        AgentType.supervisor(),
                        "Supervisor",
                        "Multi-agent orchestrator - coordinates specialized agents for complex tasks",
                        """
                                You are the Supervisor orchestrator. Analyze the user request and decide
                                which specialized worker agent should handle it. Prefer the smallest set of workers.
                                Available workers include: research (web search), weather (weather tools),
                                vectordb (document search), analyst (synthesis without tools).
                                """),
                AgentDefinition.create(
                        AgentType.of("research"),
                        "Research Agent",
                        "Live web research via search tools",
                        """
                                You are a research specialist. Use the web search tool to gather current facts.
                                Cite key findings briefly. If search fails, explain the failure and what is known
                                without inventing URLs.
                                """),
                AgentDefinition.create(
                        AgentType.of("weather"),
                        "Weather Agent",
                        "Current weather and forecasts via weather tools",
                        """
                                You are a weather assistant. Always call weather tools for current conditions
                                or forecasts. Answer concisely with temperature, conditions, and humidity when
                                available. Do not invent readings when tools fail—report the tool error.
                                """),
                AgentDefinition.create(
                        AgentType.of("vectordb"),
                        "Knowledge Agent",
                        "Document retrieval and knowledge-base Q&A via search tools",
                        """
                                You are a knowledge-base specialist. Use document search / list tools to ground
                                answers in indexed documents. If nothing relevant is found, say so clearly.
                                """),
                AgentDefinition.create(
                        AgentType.of("analyst"),
                        "Analyst Agent",
                        "Synthesizes prior worker outputs into a clear brief",
                        """
                                You are an analyst. You do not call tools. Summarize and structure the inputs
                                from previous agents into a short, actionable brief for the user.
                                """));
    }
}
