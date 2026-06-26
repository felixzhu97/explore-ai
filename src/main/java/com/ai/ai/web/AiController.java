package com.ai.ai.web;

import com.ai.ai.web.dto.*;
import com.ai.ai.application.usecase.ChatUseCase;
import com.ai.ai.application.usecase.StructuredOutputUseCasePort;
import jakarta.validation.Valid;
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

    private final ChatUseCase chatUseCase;
    private final StructuredOutputUseCasePort structuredOutputUseCase;

    public AiController(ChatUseCase chatUseCase, StructuredOutputUseCasePort structuredOutputUseCase) {
        this.chatUseCase = chatUseCase;
        this.structuredOutputUseCase = structuredOutputUseCase;
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

        String response;
        if (request.sessionId() != null && !request.sessionId().isBlank()) {
            response = chatUseCase.processChatMessage(request.sessionId(), request.message());
        } else {
            response = chatUseCase.processChatMessage(request.message());
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

        String response = chatUseCase.processChatMessage(message);
        return ResponseEntity.ok(Map.of("response", response));
    }

    /**
     * Retrieves message history for a session.
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<MessageHistoryResponse> getMessages(@PathVariable String sessionId) {
        return chatUseCase.getSession(sessionId)
            .map(session -> ResponseEntity.ok(MessageHistoryResponse.from(session)))
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Creates a new session.
     */
    @PostMapping("/sessions")
    public ResponseEntity<SessionInfo> createSession(@RequestBody(required = false) Map<String, String> body) {
        String title = body != null ? body.get("title") : null;
        var session = chatUseCase.createSession(title != null ? title : "New Chat");
        return ResponseEntity.ok(SessionInfo.from(session));
    }

    /**
     * Retrieves all sessions.
     */
    @GetMapping("/sessions")
    public ResponseEntity<List<SessionInfo>> getAllSessions() {
        List<SessionInfo> sessions = chatUseCase.getAllSessions()
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
        chatUseCase.deleteSession(sessionId);
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

        TextAnalysisResult result;
        if (request.language() != null && !request.language().isBlank()) {
            result = structuredOutputUseCase.analyzeTextWithLanguage(request.text(), request.language());
        } else {
            result = structuredOutputUseCase.analyzeText(request.text());
        }

        return ResponseEntity.ok(result);
    }
}
