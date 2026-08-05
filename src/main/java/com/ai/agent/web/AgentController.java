package com.ai.agent.web;

import com.ai.agent.application.usecase.DeepAgentUseCase;
import com.ai.agent.domain.exception.AgentSessionNotFoundException;
import com.ai.agent.web.dto.AgentInvokeRequest;
import com.ai.agent.web.dto.AgentSessionResponse;
import com.ai.agent.web.dto.CreateAgentSessionRequest;
import com.ai.common.web.ClientIdentity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.DeleteMapping;
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

    private final DeepAgentUseCase deepAgentUseCase;

    public AgentController(DeepAgentUseCase deepAgentUseCase) {
        this.deepAgentUseCase = deepAgentUseCase;
    }

    @PostMapping("/sessions")
    public ResponseEntity<AgentSessionResponse> createSession(
            @RequestBody(required = false) CreateAgentSessionRequest request,
            HttpServletRequest httpRequest) {
        String clientId = ClientIdentity.require(httpRequest);
        String title = request == null ? null : request.title();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AgentSessionResponse.from(deepAgentUseCase.createSession(title, clientId)));
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<AgentSessionResponse>> listSessions(HttpServletRequest httpRequest) {
        String clientId = ClientIdentity.require(httpRequest);
        return ResponseEntity.ok(deepAgentUseCase.listSessions(clientId).stream()
                .map(AgentSessionResponse::from)
                .toList());
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<AgentSessionResponse> getSession(
            @PathVariable String sessionId,
            HttpServletRequest httpRequest) {
        try {
            String clientId = ClientIdentity.require(httpRequest);
            return ResponseEntity.ok(AgentSessionResponse.from(deepAgentUseCase.getSession(sessionId, clientId)));
        } catch (AgentSessionNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(
            @PathVariable String sessionId,
            HttpServletRequest httpRequest) {
        try {
            deepAgentUseCase.deleteSession(sessionId, ClientIdentity.require(httpRequest));
            return ResponseEntity.noContent().build();
        } catch (AgentSessionNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(value = "/sessions/{sessionId}/invoke/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> invoke(
            @PathVariable String sessionId,
            @Valid @RequestBody AgentInvokeRequest request,
            HttpServletRequest httpRequest) {
        String clientId = ClientIdentity.require(httpRequest);
        return deepAgentUseCase.invoke(sessionId, request.message(), clientId);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "module", "agent"));
    }
}
