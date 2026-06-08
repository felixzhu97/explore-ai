package com.ai.agents.domain.agent;

import com.ai.agents.config.AgentProperties;
import com.ai.agents.domain.model.AgentRequest;
import com.ai.agents.domain.model.AgentResponse;
import com.ai.agents.domain.model.ToolResult;
import com.ai.agents.service.AgentRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Supervisor Agent that routes requests to specialized agents.
 * Uses keyword-based routing similar to the Python implementation.
 */
@Component
public class SupervisorAgent extends AbstractAiAgent {

    private static final Logger log = LoggerFactory.getLogger(SupervisorAgent.class);

    private final AgentRegistryService agentRegistryService;
    private final AgentProperties agentProperties;

    public SupervisorAgent(
            AgentRegistryService agentRegistryService,
            AgentProperties agentProperties
    ) {
        super("supervisor", "Supervisor", "Main supervisor agent that routes requests to specialized agents", AgentType.SUPERVISOR);
        this.agentRegistryService = agentRegistryService;
        this.agentProperties = agentProperties;
    }

    @Override
    protected String getSystemPrompt() {
        return agentProperties.getSupervisor().getSystemPrompt();
    }

    @Override
    protected String processWithContext(AgentRequest request, List<ToolResult> toolResults) {
        // First, determine which agent to route to
        String targetAgentType = routeToAgent(request.message());

        if (targetAgentType == null) {
            // No specific agent found, use default (chat)
            targetAgentType = agentProperties.getRouting().getDefaultAgent();
        }

        log.info("Routing request to agent: {}", targetAgentType);

        // Try to find and invoke the target agent
        Optional<AiAgent> targetAgent = agentRegistryService.findByType(targetAgentType);

        if (targetAgent.isEmpty()) {
            // Fallback to chat agent
            log.warn("Agent {} not found, falling back to chat", targetAgentType);
            targetAgent = agentRegistryService.findByType("chat");
        }

        if (targetAgent.isPresent()) {
            // Create a new request for the target agent
            AgentRequest targetRequest = new AgentRequest(request.message());
            AgentResponse targetResponse = targetAgent.get().process(targetRequest);

            if (targetResponse.error() == null) {
                return targetResponse.content();
            } else {
                return "Agent " + targetAgentType + " encountered an error: " + targetResponse.error();
            }
        }

        // No agents available, generate response directly
        return generateDirectResponse(request.message());
    }

    /**
     * Route the request to the appropriate agent based on keywords.
     */
    private String routeToAgent(String message) {
        String lowerMessage = message.toLowerCase();

        // Check for meta queries first
        if (isMetaQuery(lowerMessage)) {
            return null;
        }

        // Simple keyword matching
        if (containsAny(lowerMessage, "rag", "document", "knowledge", "retrieval")) return "rag";
        if (containsAny(lowerMessage, "tts", "speech", "voice", "audio", "synthesize")) return "tts";
        if (containsAny(lowerMessage, "vision", "image", "picture", "detect")) return "vision";
        if (containsAny(lowerMessage, "media", "generate image", "create image")) return "media";
        if (containsAny(lowerMessage, "code", "programming", "debug")) return "code";
        if (containsAny(lowerMessage, "monitor", "metric", "log", "alert")) return "monitor";
        if (containsAny(lowerMessage, "k8s", "kubernetes", "pod", "deployment")) return "k8s";

        return null;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }

    /**
     * Check if the message is a meta query (list agents, help, etc).
     */
    private boolean isMetaQuery(String message) {
        List<String> metaKeywords = List.of(
                "列出所有", "列出可用", "list all", "list available", "available agents",
                "所有 agent", "所有智能体", "有哪些 agent", "show agents", "what agents",
                "有哪些", "有什么 agent", "可用 agent", "help", "有哪些功能"
        );

        return metaKeywords.stream().anyMatch(message::contains);
    }

    /**
     * Generate a response directly when no specialized agent is available.
     */
    private String generateDirectResponse(String message) {
        return "Response: " + message + " (Note: LLM integration pending)";
    }

    /**
     * Get the list of available agents for a meta query.
     */
    public List<Map<String, String>> listAvailableAgents() {
        return agentRegistryService.listAll().stream()
                .map(agent -> Map.of(
                        "name", agent.getAgentId(),
                        "description", agent.getDescription(),
                        "status", agent.getStatus().name().toLowerCase()
                ))
                .toList();
    }
}
