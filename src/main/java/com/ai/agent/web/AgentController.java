package com.ai.agent.web;

import com.ai.agent.application.usecase.AgentFacade;
import com.ai.agent.domain.exception.AgentNotFoundException;
import com.ai.agent.web.dto.AgentHealthResponse;
import com.ai.agent.web.dto.AgentInfoResponse;
import com.ai.agent.web.dto.AgentInvokeRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agents")
public class AgentController {

    private final AgentFacade agentFacade;

    public AgentController(AgentFacade agentFacade) {
        this.agentFacade = agentFacade;
    }

    @GetMapping("/list")
    public ResponseEntity<List<AgentInfoResponse>> listAgents() {
        List<AgentInfoResponse> agents = agentFacade.listAgents().stream()
                .map(AgentInfoResponse::from)
                .toList();
        return ResponseEntity.ok(agents);
    }

    @GetMapping("/{agentType}/health")
    public ResponseEntity<?> health(@PathVariable String agentType) {
        try {
            return ResponseEntity.ok(AgentHealthResponse.from(agentFacade.health(agentType)));
        } catch (AgentNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{agentType}")
    public ResponseEntity<?> getAgent(@PathVariable String agentType) {
        try {
            return ResponseEntity.ok(AgentInfoResponse.from(agentFacade.health(agentType)));
        } catch (AgentNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(value = "/supervisor/invoke/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> invokeSupervisor(@Valid @RequestBody AgentInvokeRequest request) {
        return agentFacade.invokeSupervisor(request.message());
    }

    @PostMapping(value = "/{agentType}/invoke/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> invokeAgent(
            @PathVariable String agentType,
            @Valid @RequestBody AgentInvokeRequest request) {
        return agentFacade.invokeAgent(agentType, request.message())
                .onErrorResume(AgentNotFoundException.class, e -> Flux.just(
                        ServerSentEvent.<String>builder().event("error").data(e.getMessage()).build(),
                        ServerSentEvent.<String>builder().event("done").data("[DONE]").build()));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> moduleHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "agents", agentFacade.listAgents().size()));
    }
}
