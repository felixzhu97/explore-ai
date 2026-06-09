package com.ai.text.presentation.controller;

import com.ai.text.application.service.TextService;
import com.ai.text.presentation.dto.ChatRequest;
import com.ai.text.presentation.dto.ChatResponse;
import com.ai.text.presentation.dto.HealthResponse;
import com.ai.text.presentation.dto.ModelInfo;
import com.ai.text.presentation.dto.ProviderInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * REST controller for text/chat operations.
 * Provides endpoints for chat completion, providers, models, and health.
 */
@RestController
@RequestMapping("/api/text")
public class TextController {

    private static final Logger log = LoggerFactory.getLogger(TextController.class);

    private final TextService textService;
    private final ObjectMapper objectMapper;

    public TextController(TextService textService, ObjectMapper objectMapper) {
        this.textService = textService;
        this.objectMapper = objectMapper;
    }

    /**
     * Root endpoint with service info.
     * GET /api/text
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> root() {
        return ResponseEntity.ok(Map.of(
                "service", "AI Text Service",
                "version", "1.0.0",
                "capabilities", List.of("chat", "streaming", "providers", "models"),
                "endpoints", Map.of(
                        "providers", "/api/text/providers",
                        "models", "/api/text/models",
                        "chat", "/api/text/chat",
                        "stream", "/api/text/chat/stream",
                        "health", "/api/text/health"
                )
        ));
    }

    /**
     * List available LLM providers.
     * GET /api/text/providers
     */
    @GetMapping("/providers")
    public ResponseEntity<List<ProviderInfo>> listProviders() {
        log.debug("Listing available providers");
        List<ProviderInfo> providers = textService.getProviders();
        return ResponseEntity.ok(providers);
    }

    /**
     * List available models for a provider.
     * GET /api/text/models?provider=X
     */
    @GetMapping("/models")
    public ResponseEntity<Map<String, Object>> listModels(
            @RequestParam(required = false) String provider) {
        log.debug("Listing models for provider: {}", provider);
        List<ModelInfo> models = textService.getModels(provider);
        return ResponseEntity.ok(Map.of(
                "provider", provider != null ? provider : "openai",
                "models", models,
                "count", models.size()
        ));
    }

    /**
     * Non-streaming chat completion.
     * POST /api/text/chat
     */
    @PostMapping("/chat")
    public Mono<ResponseEntity<ChatResponse>> chat(@RequestBody ChatRequest request) {
        log.info("Received chat request: sessionId={}", request.sessionId());

        if (request.messages() == null || request.messages().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(ChatResponse.of("Error: messages are required", "unknown", "unknown", request.sessionId())));
        }

        return textService.chat(request)
                .map(response -> (ResponseEntity<ChatResponse>) ResponseEntity.ok(response))
                .onErrorResume(e -> {
                    log.error("Chat failed: {}", e.getMessage(), e);
                    ChatResponse errorResponse = ChatResponse.of(
                            "Error: " + e.getMessage(),
                            request.effectiveProvider(),
                            request.effectiveModel(),
                            request.sessionId()
                    );
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
                });
    }

    /**
     * Streaming chat completion with SSE.
     * POST /api/text/chat/stream
     *
     * Produces SSE events:
     * - event: meta, data: {"token": "..."} for each token chunk
     * - event: done, data: [DONE] when complete
     * - event: error, data: {"error": "..."} on error
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(@RequestBody ChatRequest request) {
        log.info("Received streaming chat request: sessionId={}", request.sessionId());

        if (request.messages() == null || request.messages().isEmpty()) {
            return Flux.just(ServerSentEvent.<String>builder()
                    .event("error")
                    .data("{\"error\":\"messages are required\"}")
                    .build());
        }

        return textService.streamChat(request)
                .map(sseEvent -> ServerSentEvent.<String>builder()
                        .event(sseEvent.event())
                        .data(sseEvent.data())
                        .build())
                .onErrorResume(e -> {
                    log.error("Streaming chat failed: {}", e.getMessage(), e);
                    try {
                        String errorJson = objectMapper.writeValueAsString(Map.of("error", e.getMessage()));
                        return Flux.just(
                                ServerSentEvent.<String>builder()
                                        .event("error")
                                        .data(errorJson)
                                        .build()
                        );
                    } catch (Exception ex) {
                        return Flux.just(ServerSentEvent.<String>builder()
                                .event("error")
                                .data("{\"error\":\"Internal server error\"}")
                                .build());
                    }
                });
    }

    /**
     * Health check endpoint.
     * GET /api/text/health
     */
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health(
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String model) {
        log.debug("Health check: provider={}, model={}", provider, model);

        boolean healthy = textService.isHealthy();
        String effectiveProvider = provider != null ? provider : "deepseek";
        String effectiveModel = model != null ? model : "deepseek-v4-flash";

        if (healthy) {
            return ResponseEntity.ok(HealthResponse.healthy(effectiveProvider, effectiveModel));
        } else {
            return ResponseEntity.ok(HealthResponse.degraded(effectiveProvider, effectiveModel, "LLM API unavailable"));
        }
    }
}
