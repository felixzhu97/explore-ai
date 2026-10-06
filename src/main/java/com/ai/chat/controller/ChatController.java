package com.ai.chat.controller;

import com.ai.account.controller.OwnerContext;
import com.ai.chat.controller.dto.ChatRequest;
import com.ai.chat.controller.dto.ChatResponse;
import com.ai.chat.controller.dto.CreateSessionRequest;
import com.ai.chat.controller.dto.HealthResponse;
import com.ai.chat.controller.dto.MessageInfoResponse;
import com.ai.chat.controller.dto.SessionResponse;
import com.ai.chat.controller.dto.WebSourceResponse;
import com.ai.chat.service.ChatService;
import com.ai.chat.service.SessionHistory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

  private final ChatService chatService;
  private final OwnerContext ownerContext;

  /** Reports that the chat API is up. */
  @GetMapping("/health")
  public ResponseEntity<HealthResponse> getHealth() {
    return ResponseEntity.ok(HealthResponse.up());
  }

  /** Lists the owner's chat sessions. */
  @GetMapping("/sessions")
  public ResponseEntity<List<SessionResponse>> getAllSessions(HttpServletRequest httpRequest) {
    List<SessionResponse> sessions =
        chatService.listSessions(ownerContext.requireValue(httpRequest)).stream()
            .map(SessionResponse::from)
            .toList();
    return ResponseEntity.ok(sessions);
  }

  /** Returns one chat session, or 404 when it does not exist. */
  @GetMapping("/sessions/{sessionId}")
  public ResponseEntity<SessionResponse> getSession(
      @PathVariable String sessionId, HttpServletRequest httpRequest) {
    return chatService
        .getSession(sessionId, ownerContext.requireValue(httpRequest))
        .map(session -> ResponseEntity.ok(SessionResponse.from(session)))
        .orElse(ResponseEntity.notFound().build());
  }

  /** Lists the messages of a chat session with their web sources. */
  @GetMapping("/sessions/{sessionId}/messages")
  public ResponseEntity<List<MessageInfoResponse>> getSessionMessages(
      @PathVariable String sessionId, HttpServletRequest httpRequest) {
    SessionHistory history =
        chatService.findSessionHistoryWithSources(
            sessionId, ownerContext.requireValue(httpRequest));
    List<MessageInfoResponse> messages =
        history.messages().stream()
            .map(
                message ->
                    MessageInfoResponse.from(
                        message,
                        history.sourcesFor(message).stream().map(WebSourceResponse::from).toList()))
            .toList();
    return ResponseEntity.ok(messages);
  }

  /** Sends a chat message and returns the reply. */
  @PostMapping
  public ResponseEntity<ChatResponse> chat(
      @Valid @RequestBody ChatRequest request, HttpServletRequest httpRequest) {
    String ownerKey = ownerContext.requireValue(httpRequest);
    String response;
    if (request.sessionId() != null && !request.sessionId().isBlank()) {
      response = chatService.chatWithSession(request.sessionId(), request.message(), ownerKey);
    } else {
      response = chatService.chatWithSession(request.message(), ownerKey);
    }

    return ResponseEntity.ok(ChatResponse.of(response));
  }

  /** Creates a chat session. */
  @PostMapping("/sessions")
  public ResponseEntity<SessionResponse> createSession(
      @Valid @RequestBody(required = false) CreateSessionRequest body,
      HttpServletRequest httpRequest) {
    String title = body == null ? null : body.title();
    var session = chatService.createSession(title, ownerContext.requireValue(httpRequest));
    return ResponseEntity.ok(SessionResponse.from(session));
  }

  /** Deletes a chat session. */
  @DeleteMapping("/sessions/{sessionId}")
  public ResponseEntity<Void> deleteSession(
      @PathVariable String sessionId, HttpServletRequest httpRequest) {
    chatService.deleteSession(sessionId, ownerContext.requireValue(httpRequest));
    return ResponseEntity.noContent().build();
  }
}
