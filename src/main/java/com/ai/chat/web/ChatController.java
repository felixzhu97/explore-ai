package com.ai.chat.web;

import com.ai.chat.application.usecase.ChatUseCase;
import com.ai.chat.domain.exception.ChatSessionNotFoundException;
import com.ai.chat.domain.model.ChatMessage;
import com.ai.chat.domain.repository.ChatWebSourcesRepository;
import com.ai.chat.domain.vo.ContentHash;
import com.ai.chat.domain.vo.WebSource;
import com.ai.chat.web.dto.HealthResponse;
import com.ai.chat.web.dto.*;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatUseCase chatUseCase;
    private final ChatWebSourcesRepository chatWebSourcesRepository;

    public ChatController(ChatUseCase chatUseCase, ChatWebSourcesRepository chatWebSourcesRepository) {
        this.chatUseCase = chatUseCase;
        this.chatWebSourcesRepository = chatWebSourcesRepository;
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(HealthResponse.up());
    }

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

    @PostMapping("/sessions")
    public ResponseEntity<SessionInfo> createSession(@Valid @RequestBody(required = false) CreateSessionRequest body) {
        String title = body != null && body.title() != null ? body.title() : "New Chat";
        var session = chatUseCase.createSession(title);
        return ResponseEntity.ok(SessionInfo.from(session));
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<SessionInfo>> getAllSessions() {
        List<SessionInfo> sessions = chatUseCase.getAllSessions()
            .stream()
            .map(SessionInfo::from)
            .toList();
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<SessionInfo> getSession(@PathVariable String sessionId) {
        return chatUseCase.getSession(sessionId)
                .map(session -> ResponseEntity.ok(SessionInfo.from(session)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<List<MessageInfoResponse>> getSessionMessages(@PathVariable String sessionId) {
        try {
            Map<String, List<WebSource>> sourcesByHash =
                    chatWebSourcesRepository.findByConversationId(sessionId);
            List<MessageInfoResponse> messages = chatUseCase.getSessionHistory(sessionId)
                    .stream()
                    .map(message -> toMessageInfo(message, sourcesByHash))
                    .toList();
            return ResponseEntity.ok(messages);
        } catch (ChatSessionNotFoundException e) {
            log.debug("Session not found: {}", sessionId);
            return ResponseEntity.notFound().build();
        }
    }

    private static MessageInfoResponse toMessageInfo(
            ChatMessage message,
            Map<String, List<WebSource>> sourcesByHash) {
        if (!message.isFromAssistant() || sourcesByHash.isEmpty()) {
            return MessageInfoResponse.from(message);
        }
        List<WebSource> sources = sourcesByHash.get(ContentHash.sha256(message.getText()));
        if (sources == null || sources.isEmpty()) {
            return MessageInfoResponse.from(message);
        }
        return MessageInfoResponse.from(
                message,
                sources.stream().map(WebSourceDto::from).toList());
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable String sessionId) {
        chatUseCase.deleteSession(sessionId);
        return ResponseEntity.noContent().build();
    }
}
