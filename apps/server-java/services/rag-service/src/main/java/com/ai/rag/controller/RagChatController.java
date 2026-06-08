package com.ai.rag.controller;

import com.ai.rag.model.RagChatRequest;
import com.ai.rag.model.SourceDocument;
import com.ai.rag.service.RagChatService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for RAG chat operations.
 * Provides endpoints for querying documents with RAG.
 */
@RestController
@RequestMapping("/api/rag/chat")
public class RagChatController {

    private static final Logger log = LoggerFactory.getLogger(RagChatController.class);

    private final RagChatService ragChatService;

    public RagChatController(RagChatService ragChatService) {
        this.ragChatService = ragChatService;
    }

    /**
     * Non-streaming chat endpoint.
     * POST /api/rag/chat/
     */
    @PostMapping("/")
    public Mono<ChatResponse> chat(@Valid @RequestBody RagChatRequest request) {
        log.info("Chat request: query='{}', topK={}", request.query(), request.topK());

        return Mono.fromCallable(() -> {
            String answer = ragChatService.chat(request.query(), request.topK());
            List<SourceDocument> sources = ragChatService.searchSources(request.query(), request.topK());

            return new ChatResponse(
                    answer,
                    request.session_id(),
                    sources,
                    sources.stream().map(s -> s.text()).toList()
            );
        });
    }

    /**
     * Streaming chat endpoint.
     * POST /api/rag/chat/stream
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(@Valid @RequestBody RagChatRequest request) {
        log.info("Stream chat request: query='{}', topK={}", request.query(), request.topK());

        List<SourceDocument> sources = ragChatService.searchSources(request.query(), request.topK());
        String sourcesJson = sources.stream()
                .map(s -> "{\"text\":\"" + escapeJson(s.text()) + "\",\"score\":" + s.score() + "}")
                .collect(Collectors.joining(",", "[", "]"));

        Flux<ServerSentEvent<String>> sourceEvent = Flux.just(
                ServerSentEvent.<String>builder()
                        .event("sources")
                        .data(sourcesJson)
                        .build()
        );

        Flux<ServerSentEvent<String>> contentEvents = ragChatService.streamChat(request)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());

        Flux<ServerSentEvent<String>> doneEvent = Flux.just(
                ServerSentEvent.<String>builder()
                        .data("[DONE]")
                        .build()
        );

        return Flux.concat(sourceEvent, contentEvents, doneEvent);
    }

    /**
     * Get chat history for a session.
     * GET /api/rag/chat/history/{session_id}
     */
    @GetMapping("/history/{sessionId}")
    public Mono<SessionHistory> getHistory(@PathVariable String sessionId) {
        log.info("Get history for session: {}", sessionId);
        // TODO: Implement session store for chat history
        return Mono.just(new SessionHistory(sessionId, List.of(), 0));
    }

    /**
     * Clear chat history for a session.
     * DELETE /api/rag/chat/history/{session_id}
     */
    @DeleteMapping("/history/{sessionId}")
    public Mono<VoidResult> clearHistory(@PathVariable String sessionId) {
        log.info("Clear history for session: {}", sessionId);
        // TODO: Implement session store for chat history
        return Mono.just(new VoidResult("success"));
    }

    /**
     * Ingest text directly as a document.
     * POST /api/rag/chat/ingest-text
     */
    @PostMapping("/ingest-text")
    public Mono<IngestTextResponse> ingestText(
            @RequestParam String text,
            @RequestParam(defaultValue = "Text Document") String title
    ) {
        log.info("Ingest text: title='{}', length={}", title, text.length());
        // This endpoint is handled by DocumentController for consistency
        return Mono.just(new IngestTextResponse(
                "text-" + System.currentTimeMillis(),
                title,
                1,
                "pending"
        ));
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // Response records

    public record ChatResponse(
            String answer,
            String session_id,
            List<SourceDocument> sources,
            List<String> source_texts
    ) {}

    public record SessionHistory(
            String session_id,
            List<HistoryMessage> messages,
            int total
    ) {}

    public record HistoryMessage(
            String role,
            String content,
            String timestamp,
            List<Object> sources
    ) {}

    public record VoidResult(String status) {}

    public record IngestTextResponse(
            String doc_id,
            String title,
            int chunks,
            String status
    ) {}
}
