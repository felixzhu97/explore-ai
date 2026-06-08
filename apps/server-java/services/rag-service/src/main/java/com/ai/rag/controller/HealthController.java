package com.ai.rag.controller;

import com.ai.rag.service.DocumentService;
import com.ai.rag.service.RagChatService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Health and status endpoints for RAG service.
 */
@RestController
@RequestMapping("/api/rag")
public class HealthController {

    private final DocumentService documentService;
    private final RagChatService ragChatService;

    public HealthController(DocumentService documentService, RagChatService ragChatService) {
        this.documentService = documentService;
        this.ragChatService = ragChatService;
    }

    /**
     * Basic health check.
     * GET /api/rag/health
     */
    @GetMapping("/health")
    public Mono<Map<String, Object>> health() {
        return Mono.fromCallable(() -> Map.<String, Object>of(
                "status", "ok",
                "service", "rag-service",
                "version", "1.0.0"
        ));
    }

    /**
     * Detailed health check with component status.
     * GET /api/rag/health/detailed
     */
    @GetMapping("/health/detailed")
    public Mono<Map<String, Object>> detailedHealth() {
        return Mono.fromCallable(() -> {
            Map<String, Object> vectorStats = ragChatService.getStats();

            return Map.<String, Object>of(
                    "status", "ok",
                    "service", "rag-service",
                    "version", "1.0.0",
                    "components", Map.of(
                            "documents", Map.of(
                                    "count", documentService.count()
                            ),
                            "vector_store", vectorStats
                    )
            );
        });
    }

    /**
     * Service info endpoint.
     * GET /api/rag/info
     */
    @GetMapping("/info")
    public Mono<Map<String, Object>> info() {
        return Mono.fromCallable(() -> Map.of(
                "name", "RAG Service",
                "version", "1.0.0",
                "description", "Retrieval Augmented Generation service with Qdrant vector store",
                "endpoints", Map.of(
                        "documents", Map.of(
                                "upload", "POST /api/rag/documents/upload",
                                "list", "GET /api/rag/documents/",
                                "get", "GET /api/rag/documents/{doc_id}",
                                "delete", "DELETE /api/rag/documents/{doc_id}"
                        ),
                        "chat", Map.of(
                                "query", "POST /api/rag/chat/",
                                "stream", "POST /api/rag/chat/stream",
                                "history", "GET /api/rag/chat/history/{session_id}"
                        ),
                        "ingest", Map.of(
                                "text", "POST /api/rag/chat/ingest-text",
                                "url", "POST /api/rag/documents/ingest-url"
                        )
                )
        ));
    }
}
