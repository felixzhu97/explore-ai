package com.ai.agents.controller;

import com.ai.agents.domain.model.AgentInfo;
import com.ai.agents.domain.model.AgentRequest;
import com.ai.agents.domain.model.AgentResponse;
import com.ai.agents.domain.model.ChatMessage;
import com.ai.agents.domain.model.RouteRequest;
import com.ai.agents.domain.model.RouteResponse;
import com.ai.agents.service.AgentOrchestrationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for AI Agents API.
 * Implements all endpoints from the Python FastAPI service.
 */
@RestController
@RequestMapping("/api/agents")
public class AiAgentController {

    private static final Logger log = LoggerFactory.getLogger(AiAgentController.class);

    private final AgentOrchestrationService orchestrationService;

    public AiAgentController(AgentOrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    /**
     * Health check endpoint.
     * GET /health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        List<AgentInfo> agents = orchestrationService.listAgents();
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "service", "ai_agents",
                "agents_initialized", !agents.isEmpty(),
                "available_agents", agents.stream().map(AgentInfo::name).toList(),
                "agent_count", agents.size()
        ));
    }

    /**
     * List all available agents.
     * GET /api/agents
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listAgents() {
        List<AgentInfo> agents = orchestrationService.listAgents();
        return ResponseEntity.ok(Map.of(
                "agents", agents
        ));
    }

    /**
     * Get specific agent info.
     * GET /api/agents/{agent_id}
     */
    @GetMapping("/{agentId}")
    public ResponseEntity<AgentInfo> getAgent(@PathVariable String agentId) {
        return orchestrationService.getAgent(agentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get agent status.
     * GET /api/agents/{agent_id}/status
     */
    @GetMapping("/{agentId}/status")
    public ResponseEntity<Map<String, String>> getAgentStatus(@PathVariable String agentId) {
        String status = orchestrationService.getAgentStatus(agentId);
        if ("not_found".equals(status)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of(
                "agent_id", agentId,
                "status", status
        ));
    }

    /**
     * Route a message to the appropriate agent.
     * POST /api/agents/supervisor/route
     */
    @PostMapping("/supervisor/route")
    public ResponseEntity<RouteResponse> route(@Valid @RequestBody RouteRequest request) {
        RouteResponse response = orchestrationService.route(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Chat with the Supervisor agent.
     * POST /api/agents/supervisor/chat
     */
    @PostMapping(value = "/supervisor/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> supervisorChat(@Valid @RequestBody SupervisorChatRequest request) {
        log.info("Supervisor chat request: {}", request.message());

        AgentRequest agentRequest = new AgentRequest(request.message());

        AgentResponse response = orchestrationService.processThroughSupervisor(agentRequest);

        if (response.error() == null) {
            return Flux.just(
                    "event: message\n",
                    "data: " + escapeForSSE(response.content()) + "\n\n",
                    "data: [DONE]\n\n"
            );
        } else {
            return Flux.just(
                    "event: error\n",
                    "data: " + escapeForSSE(response.error()) + "\n\n",
                    "data: [DONE]\n\n"
            );
        }
    }

    /**
     * Chat with a specific agent.
     * POST /api/agents/{agent_id}/chat
     */
    @PostMapping(value = "/{agentId}/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> agentChat(@PathVariable String agentId, @Valid @RequestBody AgentChatRequest request) {
        log.info("Agent chat request for {}: {}", agentId, request.message());

        AgentRequest agentRequest = new AgentRequest(request.message());

        AgentResponse response = orchestrationService.processDirect(agentId, agentRequest);

        if (response.error() == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("event: message\n");
            sb.append("data: ").append(escapeForSSE(response.content())).append("\n\n");
            sb.append("data: [DONE]\n\n");
            return Flux.just(sb.toString());
        } else {
            return Flux.just(
                    "event: error\n",
                    "data: " + escapeForSSE(response.error()) + "\n\n",
                    "data: [DONE]\n\n"
            );
        }
    }

    /**
     * List available workflows (placeholder).
     * GET /workflows
     */
    @GetMapping("/workflows")
    public ResponseEntity<Map<String, Object>> listWorkflows() {
        return ResponseEntity.ok(Map.of(
                "workflows", List.of(
                        Map.of(
                                "name", "supervisor_routing",
                                "description", "Route requests to specialized agents"
                        )
                )
        ));
    }

    /**
     * Escape special characters for SSE.
     */
    private String escapeForSSE(String content) {
        if (content == null) return "";
        return content
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("data:", "data:"); // Prevent SSE injection
    }

    /**
     * Request body for supervisor chat.
     */
    public record SupervisorChatRequest(
            String message,
            List<ChatMessage> history,
            String sessionId
    ) {}

    /**
     * Request body for agent chat.
     */
    public record AgentChatRequest(
            String message,
            List<ChatMessage> history,
            String sessionId
    ) {}
}
