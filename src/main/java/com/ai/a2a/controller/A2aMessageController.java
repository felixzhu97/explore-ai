package com.ai.a2a.controller;

import com.ai.a2a.controller.dto.A2aSendMessageRequest;
import com.ai.a2a.domain.model.A2aMessage;
import com.ai.a2a.domain.model.A2aTask;
import com.ai.a2a.service.A2aService;
import com.ai.account.controller.OwnerContext;
import com.ai.agent.service.AgentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/** A2A HTTP+JSON message endpoints (IAM Bearer + Owner Key). */
@RestController
@RequestMapping("/a2a/v1")
@RequiredArgsConstructor
public class A2aMessageController {

  private final A2aService a2aService;
  private final OwnerContext ownerContext;

  /** Synchronous SendMessage → Task. */
  @PostMapping("/message:send")
  public ResponseEntity<Map<String, Object>> sendMessage(
      @Valid @RequestBody A2aSendMessageRequest body,
      @RequestParam(value = "lang", required = false) String lang,
      HttpServletRequest request) {
    String ownerKey = ownerContext.requireValue(request);
    String language = AgentService.resolveLanguage(lang, request.getHeader("Accept-Language"));
    A2aTask task = a2aService.sendMessage(toMessage(body), ownerKey, language);
    return ResponseEntity.ok(task.toWireMap());
  }

  /** Streaming SendStreamingMessage → SSE Task updates. */
  @PostMapping(value = "/message:stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<ServerSentEvent<Map<String, Object>>> streamMessage(
      @Valid @RequestBody A2aSendMessageRequest body,
      @RequestParam(value = "lang", required = false) String lang,
      HttpServletRequest request) {
    String ownerKey = ownerContext.requireValue(request);
    String language = AgentService.resolveLanguage(lang, request.getHeader("Accept-Language"));
    return a2aService.streamMessage(toMessage(body), ownerKey, language);
  }

  private static A2aMessage toMessage(A2aSendMessageRequest body) {
    A2aSendMessageRequest.A2aMessageBody message = body.message();
    return A2aMessage.createMessage(
        message.messageId(), message.role(), message.parts(), message.metadata());
  }
}
