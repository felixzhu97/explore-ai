package com.ai.pipeline.controller;

import com.ai.account.controller.OwnerContext;
import com.ai.common.controller.dto.HealthStatus;
import com.ai.pipeline.controller.dto.AgentHealthResponse;
import com.ai.pipeline.controller.dto.AgentInfoResponse;
import com.ai.pipeline.controller.dto.AgentInvokeRequest;
import com.ai.pipeline.controller.dto.PipelineInvokeRequest;
import com.ai.pipeline.controller.dto.PipelineModuleHealthResponse;
import com.ai.pipeline.domain.model.AgentPipeline;
import com.ai.pipeline.domain.vo.AgentType;
import com.ai.pipeline.service.PipelineService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
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

@RestController
@RequestMapping("/api/pipelines")
@RequiredArgsConstructor
public class PipelineController {

  private final PipelineService pipelineService;
  private final OwnerContext ownerContext;

  /** Lists the agents. */
  @GetMapping("/agent-types")
  public ResponseEntity<List<AgentInfoResponse>> listAgents(
      @RequestParam(value = "lang", required = false) String lang, HttpServletRequest request) {
    String ownerKey = ownerContext.requireValue(request);
    String language = resolveLanguage(lang, request);
    List<AgentInfoResponse> agents =
        pipelineService.listAgents(ownerKey, language).stream()
            .map(AgentInfoResponse::from)
            .toList();
    return ResponseEntity.ok(agents);
  }

  /** Returns the pipeline module health. */
  @GetMapping("/health")
  public ResponseEntity<PipelineModuleHealthResponse> getModuleHealth() {
    return ResponseEntity.ok(
        new PipelineModuleHealthResponse(HealthStatus.UP, pipelineService.countBuiltins()));
  }

  /** Returns one agent. */
  @GetMapping("/{agentType}")
  public ResponseEntity<AgentInfoResponse> getAgent(
      @PathVariable String agentType,
      @RequestParam(value = "lang", required = false) String lang,
      HttpServletRequest request) {
    String ownerKey = ownerContext.requireValue(request);
    return ResponseEntity.ok(
        AgentInfoResponse.from(
            pipelineService.getHealth(agentType, ownerKey, resolveLanguage(lang, request))));
  }

  /** Returns the health of one agent. */
  @GetMapping("/{agentType}/health")
  public ResponseEntity<AgentHealthResponse> getHealth(
      @PathVariable String agentType,
      @RequestParam(value = "lang", required = false) String lang,
      HttpServletRequest request) {
    String ownerKey = ownerContext.requireValue(request);
    return ResponseEntity.ok(
        AgentHealthResponse.from(
            pipelineService.getHealth(agentType, ownerKey, resolveLanguage(lang, request))));
  }

  /** Streams a supervisor run. */
  @PostMapping(value = "/supervisor/invoke/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<ServerSentEvent<String>> invokeSupervisor(
      @Valid @RequestBody AgentInvokeRequest request,
      @RequestParam(value = "lang", required = false) String lang,
      HttpServletRequest httpRequest) {
    String ownerKey = ownerContext.requireValue(httpRequest);
    return pipelineService.invokeSupervisor(
        request.message(), ownerKey, resolveLanguage(lang, httpRequest));
  }

  /** Streams a pipeline run. */
  @PostMapping(value = "/invoke/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<ServerSentEvent<String>> invokePipeline(
      @Valid @RequestBody PipelineInvokeRequest request,
      @RequestParam(value = "lang", required = false) String lang,
      HttpServletRequest httpRequest) {
    String ownerKey = ownerContext.requireValue(httpRequest);
    List<AgentPipeline.PipelineNode> nodes =
        request.nodes().stream()
            .map(
                node ->
                    new AgentPipeline.PipelineNode(
                        node.id(),
                        AgentType.of(node.agentType()),
                        node.name(),
                        node.description(),
                        node.systemPrompt(),
                        node.toolKeys() == null ? List.of() : node.toolKeys()))
            .toList();
    List<AgentPipeline.PipelineEdge> edges =
        request.edges().stream()
            .map(edge -> new AgentPipeline.PipelineEdge(edge.sourceId(), edge.targetId()))
            .toList();
    return pipelineService.invokePipeline(
        request.message(),
        AgentPipeline.create(nodes, edges),
        ownerKey,
        resolveLanguage(lang, httpRequest));
  }

  /** Streams a single agent run. */
  @PostMapping(value = "/{agentType}/invoke/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<ServerSentEvent<String>> invokeAgent(
      @PathVariable String agentType,
      @Valid @RequestBody AgentInvokeRequest request,
      @RequestParam(value = "lang", required = false) String lang,
      HttpServletRequest httpRequest) {
    String ownerKey = ownerContext.requireValue(httpRequest);
    return pipelineService.invokeAgent(
        agentType, request.message(), ownerKey, resolveLanguage(lang, httpRequest));
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
