package com.ai.adapter.in.controller;

import com.ai.domain.service.AiChatService;
import com.ai.domain.service.StructuredOutputService;
import com.ai.adapter.in.dto.ChatRequest;
import com.ai.adapter.in.dto.ChatResponse;
import com.ai.adapter.in.dto.MessageHistoryResponse;
import com.ai.adapter.in.dto.SessionInfo;
import com.ai.adapter.in.dto.TextAnalysisRequest;
import com.ai.adapter.in.dto.TextAnalysisResult;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Chat REST Controller.
 */
@RestController
@RequestMapping("/api")
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    private final AiChatService chatService;
    private final StructuredOutputService structuredOutputService;

    public AiController(AiChatService chatService, StructuredOutputService structuredOutputService) {
        this.chatService = chatService;
        this.structuredOutputService = structuredOutputService;
    }

    /**
     * Sends a chat message and receives AI response.
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        if (request.message() == null || request.message().isBlank()) {
            return ResponseEntity.badRequest()
                .body(ChatResponse.of("Please provide a message."));
        }

        log.info("Received chat request: {}", truncate(request.message()));

        String response;
        if (request.sessionId() != null && !request.sessionId().isBlank()) {
            response = chatService.processChatMessage(request.sessionId(), request.message());
        } else {
            response = chatService.processChatMessage(request.message());
        }

        return ResponseEntity.ok(ChatResponse.of(response));
    }

    /**
     * Sends a simple chat message (legacy API compatibility).
     */
    @PostMapping("/chat/simple")
    public ResponseEntity<Map<String, String>> chatSimple(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest()
                .body(Map.of("response", "Please provide a message."));
        }

        String response = chatService.processChatMessage(message);
        return ResponseEntity.ok(Map.of("response", response));
    }

    /**
     * Retrieves message history for a session.
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<MessageHistoryResponse> getMessages(@PathVariable String sessionId) {
        return chatService.getSession(sessionId)
            .map(session -> ResponseEntity.ok(MessageHistoryResponse.from(session)))
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Creates a new session.
     */
    @PostMapping("/sessions")
    public ResponseEntity<SessionInfo> createSession(@RequestBody(required = false) Map<String, String> body) {
        String title = body != null ? body.get("title") : null;
        var session = chatService.createSession(title != null ? title : "New Chat");
        return ResponseEntity.ok(SessionInfo.from(session));
    }

    /**
     * Retrieves all sessions.
     */
    @GetMapping("/sessions")
    public ResponseEntity<List<SessionInfo>> getAllSessions() {
        List<SessionInfo> sessions = chatService.getAllSessions()
            .stream()
            .map(SessionInfo::from)
            .toList();
        return ResponseEntity.ok(sessions);
    }

    /**
     * Deletes a session.
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable String sessionId) {
        chatService.deleteSession(sessionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    /**
     * Analyzes text and returns structured result.
     * Demonstrates Spring AI 2.0 structured output with .entity() method.
     */
    @PostMapping("/chat/analyze")
    public ResponseEntity<TextAnalysisResult> analyzeText(@Valid @RequestBody TextAnalysisRequest request) {
        if (request.text() == null || request.text().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        log.info("Text analysis request: {} chars", request.text().length());

        TextAnalysisResult result;
        if (request.language() != null && !request.language().isBlank()) {
            result = structuredOutputService.analyzeTextWithLanguage(request.text(), request.language());
        } else {
            result = structuredOutputService.analyzeText(request.text());
        }

        return ResponseEntity.ok(result);
    }

    private String truncate(String text) {
        if (text == null) return "null";
        if (text.length() <= 50) return text;
        return text.substring(0, 50) + "...";
    }
}
