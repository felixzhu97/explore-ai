package com.ai.chat.web;

import com.ai.chat.application.usecase.ChatFacade;
import com.ai.chat.domain.exception.ChatSessionNotFoundException;
import com.ai.chat.web.dto.*;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Chat REST Controller.
 * Handles chat, session management, and health endpoints.
 */
@RestController
@RequestMapping("/api")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatFacade chatFacade;

    public ChatController(ChatFacade chatFacade) {
        this.chatFacade = chatFacade;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        if (request.message() == null || request.message().isBlank()) {
            return ResponseEntity.badRequest()
                .body(ChatResponse.of("Please provide a message."));
        }

        String response;
        if (request.sessionId() != null && !request.sessionId().isBlank()) {
            response = chatFacade.chatWithSession(request.sessionId(), request.message());
        } else {
            response = chatFacade.chatWithSession(request.message());
        }

        return ResponseEntity.ok(ChatResponse.of(response));
    }

    @PostMapping("/sessions")
    public ResponseEntity<SessionInfo> createSession(@Valid @RequestBody(required = false) CreateSessionRequest body) {
        String title = body != null && body.title() != null ? body.title() : "New Chat";
        var session = chatFacade.createSession(title);
        return ResponseEntity.ok(SessionInfo.from(session));
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<SessionInfo>> getAllSessions() {
        List<SessionInfo> sessions = chatFacade.getAllSessions()
            .stream()
            .map(SessionInfo::from)
            .toList();
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<SessionInfo> getSession(@PathVariable String sessionId) {
        return chatFacade.getSession(sessionId)
                .map(session -> ResponseEntity.ok(SessionInfo.from(session)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<List<MessageInfoResponse>> getSessionMessages(@PathVariable String sessionId) {
        try {
            List<MessageInfoResponse> messages = chatFacade.getSessionHistory(sessionId)
                    .stream()
                    .map(MessageInfoResponse::from)
                    .toList();
            return ResponseEntity.ok(messages);
        } catch (ChatSessionNotFoundException e) {
            log.debug("Session not found: {}", sessionId);
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable String sessionId) {
        chatFacade.deleteSession(sessionId);
        return ResponseEntity.noContent().build();
    }
}
