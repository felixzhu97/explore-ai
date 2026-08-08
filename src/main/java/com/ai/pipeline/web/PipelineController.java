package com.ai.pipeline.web;

import com.ai.account.web.OwnerContext;
import com.ai.common.web.ClientIdentity;
import com.ai.pipeline.application.usecase.PipelineFacade;
import com.ai.pipeline.domain.exception.AgentNotFoundException;
import com.ai.pipeline.domain.model.AgentPipeline;
import com.ai.pipeline.domain.vo.AgentType;
import com.ai.pipeline.web.dto.AgentHealthResponse;
import com.ai.pipeline.web.dto.AgentInfoResponse;
import com.ai.pipeline.web.dto.AgentInvokeRequest;
import com.ai.pipeline.web.dto.PipelineInvokeRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/pipelines")
public class PipelineController {

    private final OwnerContext ownerContext;

    private final PipelineFacade agentFacade;

    public PipelineController(PipelineFacade agentFacade, OwnerContext ownerContext) {
        this.ownerContext = ownerContext;
        this.agentFacade = agentFacade;
    }

    @GetMapping("/list")
    public ResponseEntity<List<AgentInfoResponse>> listAgents(
            @RequestParam(value = "lang", required = false) String lang,
            HttpServletRequest request) {
        String clientId = ownerContext.requireValue(request);
        String language = resolveLanguage(lang, request);
        List<AgentInfoResponse> agents = agentFacade.listAgents(clientId, language).stream()
                .map(AgentInfoResponse::from)
                .toList();
        return ResponseEntity.ok(agents);
    }

    @GetMapping("/{agentType}/health")
    public ResponseEntity<?> health(
            @PathVariable String agentType,
            @RequestParam(value = "lang", required = false) String lang,
            HttpServletRequest request) {
        try {
            String clientId = ownerContext.requireValue(request);
            return ResponseEntity.ok(AgentHealthResponse.from(
                    agentFacade.health(agentType, clientId, resolveLanguage(lang, request))));
        } catch (AgentNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{agentType}")
    public ResponseEntity<?> getAgent(
            @PathVariable String agentType,
            @RequestParam(value = "lang", required = false) String lang,
            HttpServletRequest request) {
        try {
            String clientId = ownerContext.requireValue(request);
            return ResponseEntity.ok(AgentInfoResponse.from(
                    agentFacade.health(agentType, clientId, resolveLanguage(lang, request))));
        } catch (AgentNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(value = "/supervisor/invoke/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> invokeSupervisor(
            @Valid @RequestBody AgentInvokeRequest request,
            @RequestParam(value = "lang", required = false) String lang,
            HttpServletRequest httpRequest) {
        String clientId = ownerContext.requireValue(httpRequest);
        return agentFacade.invokeSupervisor(request.message(), clientId, resolveLanguage(lang, httpRequest));
    }

    @PostMapping(value = "/invoke/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> invokePipeline(
            @Valid @RequestBody PipelineInvokeRequest request,
            @RequestParam(value = "lang", required = false) String lang,
            HttpServletRequest httpRequest) {
        String clientId = ownerContext.requireValue(httpRequest);
        List<AgentPipeline.PipelineNode> nodes = request.nodes().stream()
                .map(node -> new AgentPipeline.PipelineNode(
                        node.id(),
                        AgentType.of(node.agentType()),
                        node.name(),
                        node.description(),
                        node.systemPrompt(),
                        node.toolKeys() == null ? List.of() : node.toolKeys()))
                .toList();
        List<AgentPipeline.PipelineEdge> edges = request.edges().stream()
                .map(edge -> new AgentPipeline.PipelineEdge(edge.sourceId(), edge.targetId()))
                .toList();
        return agentFacade.invokePipeline(
                request.message(),
                AgentPipeline.create(nodes, edges),
                clientId,
                resolveLanguage(lang, httpRequest));
    }

    @PostMapping(value = "/{agentType}/invoke/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> invokeAgent(
            @PathVariable String agentType,
            @Valid @RequestBody AgentInvokeRequest request,
            @RequestParam(value = "lang", required = false) String lang,
            HttpServletRequest httpRequest) {
        String clientId = ownerContext.requireValue(httpRequest);
        return agentFacade.invokeAgent(agentType, request.message(), clientId, resolveLanguage(lang, httpRequest))
                .onErrorResume(AgentNotFoundException.class, e -> Flux.just(
                        ServerSentEvent.<String>builder().event("error").data(e.getMessage()).build(),
                        ServerSentEvent.<String>builder().event("done").data("[DONE]").build()));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> moduleHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "agents", agentFacade.builtinCount()));
    }

    private static String resolveLanguage(String lang, HttpServletRequest request) {
        if (lang != null && !lang.isBlank()) {
            return lang;
        }
        String acceptLanguage = request.getHeader("Accept-Language");
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return Locale.ENGLISH.getLanguage();
        }
        return acceptLanguage;
    }
}
