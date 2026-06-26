package com.ai.ai.web;

import com.ai.ai.web.dto.*;
import com.ai.ai.application.usecase.ChatUseCase;
import com.ai.ai.application.usecase.StructuredOutputUseCasePort;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
            response = chatUseCase.chatWithSession(request.sessionId(), request.message());
        } else {
            response = chatUseCase.chatWithSession(request.message());
        }

        return ResponseEntity.ok(ChatResponse.of(response));
    }

    /**
     * Sends a simple chat message (legacy API compatibility).
     * @deprecated Use {@link #chat(ChatRequest)} instead.
     */
    @Deprecated
    @PostMapping("/chat/simple")
    public ResponseEntity<SimpleChatResponse> chatSimple(@Valid @RequestBody SimpleChatRequest request) {
        if (request.message() == null || request.message().isBlank()) {
            return ResponseEntity.badRequest()
                .body(SimpleChatResponse.of("Please provide a message."));
        }

        String response = chatUseCase.chatWithSession(request.message());
        return ResponseEntity.ok(SimpleChatResponse.of(response));
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
    public ResponseEntity<SessionInfo> createSession(@Valid @RequestBody(required = false) CreateSessionRequest body) {
        String title = body != null && body.title() != null ? body.title() : "New Chat";
        var session = chatUseCase.createSession(title);
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
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(HealthResponse.up());
    }

    /**
     * Analyzes text and returns structured result.
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
