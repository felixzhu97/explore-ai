package com.ai.agent.controller;

import com.ai.account.controller.OwnerContext;
import com.ai.agent.controller.dto.AgentInvokeBody;
import com.ai.agent.controller.dto.AgentInvokeResponse;
import com.ai.agent.controller.dto.AgentSummaryResponse;
import com.ai.agent.service.AgentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
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

/** Published Language for sibling products: list and invoke catalog Agents. */
@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentController {

  private final AgentService agentService;
  private final OwnerContext ownerContext;

  /** Lists runnable Agent types. */
  @GetMapping
  public ResponseEntity<List<AgentSummaryResponse>> listAgents(
      @RequestParam(value = "lang", required = false) String lang, HttpServletRequest request) {
    String ownerKey = ownerContext.requireValue(request);
    String language = AgentService.resolveLanguage(lang, request.getHeader("Accept-Language"));
    List<AgentSummaryResponse> agents =
        agentService.listAgents(ownerKey, language).stream()
            .map(AgentSummaryResponse::createResponse)
            .toList();
    return ResponseEntity.ok(agents);
  }

  /** Returns one Agent summary. */
  @GetMapping("/{agentType}")
  public ResponseEntity<AgentSummaryResponse> getAgent(
      @PathVariable String agentType,
      @RequestParam(value = "lang", required = false) String lang,
      HttpServletRequest request) {
    String ownerKey = ownerContext.requireValue(request);
    String language = AgentService.resolveLanguage(lang, request.getHeader("Accept-Language"));
    return ResponseEntity.ok(
        AgentSummaryResponse.createResponse(agentService.getAgent(agentType, ownerKey, language)));
  }

  /** Runs one Agent and returns the reply. */
  @PostMapping("/{agentType}/invoke")
  public ResponseEntity<AgentInvokeResponse> invoke(
      @PathVariable String agentType,
      @Valid @RequestBody AgentInvokeBody body,
      @RequestParam(value = "lang", required = false) String lang,
      HttpServletRequest request) {
    String ownerKey = ownerContext.requireValue(request);
    String language = AgentService.resolveLanguage(lang, request.getHeader("Accept-Language"));
    return ResponseEntity.ok(
        AgentInvokeResponse.createResponse(
            agentService.invoke(agentType, body.message(), ownerKey, language)));
  }

  /** Streams one Agent run as SSE. */
  @PostMapping(value = "/{agentType}/invoke/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<ServerSentEvent<String>> invokeStream(
      @PathVariable String agentType,
      @Valid @RequestBody AgentInvokeBody body,
      @RequestParam(value = "lang", required = false) String lang,
      HttpServletRequest request) {
    String ownerKey = ownerContext.requireValue(request);
    String language = AgentService.resolveLanguage(lang, request.getHeader("Accept-Language"));
    return agentService.invokeStream(agentType, body.message(), ownerKey, language);
  }
}
