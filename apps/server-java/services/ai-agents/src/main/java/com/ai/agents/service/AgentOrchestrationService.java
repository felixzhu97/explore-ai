package com.ai.agents.service;

import com.ai.agents.config.AgentProperties;
import com.ai.agents.domain.agent.*;
import com.ai.agents.domain.model.AgentInfo;
import com.ai.agents.domain.model.AgentRequest;
import com.ai.agents.domain.model.AgentResponse;
import com.ai.agents.domain.model.RouteRequest;
import com.ai.agents.domain.model.RouteResponse;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Main orchestration service for AI Agents.
 * Coordinates between the Supervisor and specialized agents.
 */
@Service
public class AgentOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(AgentOrchestrationService.class);

    private final AgentRegistryService agentRegistryService;
    private final SupervisorAgent supervisorAgent;
    private final AgentProperties agentProperties;

    public AgentOrchestrationService(
            AgentRegistryService agentRegistryService,
            SupervisorAgent supervisorAgent,
            AgentProperties agentProperties
    ) {
        this.agentRegistryService = agentRegistryService;
        this.supervisorAgent = supervisorAgent;
        this.agentProperties = agentProperties;
    }

    @PostConstruct
    public void initialize() {
        // Register supervisor
        agentRegistryService.register(supervisorAgent);
        log.info("Agent orchestration service initialized");
    }

    /**
     * Route a message to the appropriate agent (Supervisor routing).
     */
    public RouteResponse route(RouteRequest request) {
        String message = request.message();
        String lowerMessage = message.toLowerCase();

        // Check for meta queries (list agents)
        if (isMetaQuery(lowerMessage)) {
            return new RouteResponse("supervisor", "Meta query - list available agents", 1.0,
                    agentRegistryService.listAll().stream()
                            .map(AiAgent::getAgentId)
                            .collect(Collectors.toList()));
        }

        // Route based on keywords
        for (Map.Entry<AgentType, List<String>> entry : getRoutingKeywords().entrySet()) {
            for (String keyword : entry.getValue()) {
                if (lowerMessage.contains(keyword)) {
                    return RouteResponse.of(entry.getKey().getId(), "Matched keyword: " + keyword);
                }
            }
        }

        // Default to chat agent
        return RouteResponse.of("chat", "Default routing", 0.5);
    }

    /**
     * Process a request through the Supervisor.
     */
    public AgentResponse processThroughSupervisor(AgentRequest request) {
        return supervisorAgent.process(request);
    }

    /**
     * Process a request directly through a specific agent.
     */
    public AgentResponse processDirect(String agentId, AgentRequest request) {
        Optional<AiAgent> agent = agentRegistryService.findById(agentId);

        if (agent.isEmpty()) {
            // Try by type
            agent = agentRegistryService.findByType(agentId);
        }

        if (agent.isEmpty()) {
            return AgentResponse.error("Agent not found: " + agentId);
        }

        return agent.get().process(request);
    }

    /**
     * Get all available agents.
     */
    public List<AgentInfo> listAgents() {
        return agentRegistryService.listAll().stream()
                .map(agent -> AgentInfo.of(
                        agent.getAgentId(),
                        agent.getName(),
                        agent.getDescription(),
                        agent.getStatus().name().toLowerCase(),
                        agent.getAgentType().getId()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Get agent by ID.
     */
    public Optional<AgentInfo> getAgent(String agentId) {
        return agentRegistryService.findById(agentId)
                .map(agent -> AgentInfo.of(
                        agent.getAgentId(),
                        agent.getName(),
                        agent.getDescription(),
                        agent.getStatus().name().toLowerCase(),
                        agent.getAgentType().getId()
                ));
    }

    /**
     * Get agent status.
     */
    public String getAgentStatus(String agentId) {
        return agentRegistryService.findById(agentId)
                .map(agent -> agent.getStatus().name().toLowerCase())
                .orElse("not_found");
    }

    /**
     * Get routing keywords map.
     */
    private Map<AgentType, List<String>> getRoutingKeywords() {
        return Map.of(
                AgentType.RAG, List.of("rag", "document", "knowledge", "retrieval", "search docs"),
                AgentType.TTS, List.of("tts", "speech", "voice", "text to speech"),
                AgentType.VISION, List.of("vision", "image", "picture", "detect"),
                AgentType.MEDIA, List.of("media", "generate image", "image generation"),
                AgentType.CODE, List.of("code", "programming", "debug"),
                AgentType.DATA, List.of("data", "analytics", "statistics"),
                AgentType.MONITOR, List.of("monitor", "metric", "log", "alert"),
                AgentType.K8S, List.of("k8s", "kubernetes", "pod", "deployment"),
                AgentType.AIOPS, List.of("aiops", "incident", "root cause"),
                AgentType.VIDEO, List.of("video", "generate video", "animation")
        );
    }

    /**
     * Check if message is a meta query.
     */
    private boolean isMetaQuery(String message) {
        List<String> metaKeywords = List.of(
                "列出所有", "列出可用", "list all", "list available", "available agents",
                "所有 agent", "所有智能体", "有哪些 agent", "show agents", "what agents",
                "有哪些", "有什么 agent", "可用 agent", "help"
        );
        return metaKeywords.stream().anyMatch(message::contains);
    }
}
