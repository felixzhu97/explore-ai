package com.ai.interfaces.controller;

import com.ai.application.service.LanguageDetectionService;
import com.ai.application.service.RagApplicationService;
import com.ai.domain.model.Document;
import com.ai.domain.model.SourceDocument;
import com.ai.domain.service.AiChatService;
import com.ai.infrastructure.adapter.document.PdfTextExtractor;
import com.ai.infrastructure.event.SourcesEvent;
import com.ai.interfaces.dto.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * RAG REST controller.
 * Handles document management and RAG chat endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/api/rag")
@Tag(name = "RAG", description = "RAG document management and chat")
public class RagController {

    private final RagApplicationService ragApplicationService;
    private final LanguageDetectionService languageDetectionService;
    private final AiChatService aiChatService;
    private final ObjectMapper objectMapper;
    private final PdfTextExtractor pdfTextExtractor;

    public RagController(RagApplicationService ragApplicationService,
                         LanguageDetectionService languageDetectionService,
                         AiChatService aiChatService,
                         ObjectMapper objectMapper,
                         PdfTextExtractor pdfTextExtractor) {
        this.ragApplicationService = ragApplicationService;
        this.languageDetectionService = languageDetectionService;
        this.aiChatService = aiChatService;
        this.objectMapper = objectMapper;
        this.pdfTextExtractor = pdfTextExtractor;
    }

    /**
     * List all documents.
     */
    @GetMapping("/documents/")
    @Operation(summary = "List all documents", description = "Returns all uploaded documents")
    public ResponseEntity<DocumentListResponse> listDocuments() {
        log.info("Listing all documents");
        
        List<Document> documents = ragApplicationService.listDocuments();
        List<DocumentSummaryDto> summaries = documents.stream()
            .map(this::toDocumentSummaryDto)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(new DocumentListResponse(summaries));
    }

    /**
     * Upload a new document.
     */
    @PostMapping("/documents/upload")
    @Operation(summary = "Upload a document", description = "Upload a new document for RAG processing")
    public ResponseEntity<UploadDocumentResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title) {
        String fileName = file.getOriginalFilename();
        String docTitle = title != null ? title : fileName;
        
        String content;
        try {
            byte[] fileBytes = file.getBytes();
            String extension = pdfTextExtractor.getExtension(fileName);
            
            if ("pdf".equalsIgnoreCase(extension)) {
                // Extract text from PDF using PDFBox
                var extractedText = pdfTextExtractor.extractText(fileBytes);
                if (extractedText.isEmpty()) {
                    throw new RuntimeException("Failed to extract text from PDF file: " + fileName);
                }
                content = extractedText.get();
                log.info("Extracted {} characters from PDF: {}", content.length(), fileName);
            } else {
                // For other text files, read as UTF-8
                content = new String(fileBytes, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.error("Failed to read file content", e);
            throw new RuntimeException("Failed to read file content: " + e.getMessage(), e);
        }
        
        log.info("Uploading document: {}", docTitle);
        
        Document document = ragApplicationService.uploadDocument(
            docTitle,
            fileName,
            file.getSize(),
            content
        );
        
        UploadDocumentResponse response = new UploadDocumentResponse(
            document.getId().value(),
            document.getTitle(),
            document.getStatus().name(),
            0, // chunkCount not available from Document model
            document.getCreatedAt()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Delete a document by ID.
     */
    @DeleteMapping("/documents/{id}")
    @Operation(summary = "Delete a document", description = "Delete a document by its ID")
    public ResponseEntity<Void> deleteDocument(@PathVariable UUID id) {
        log.info("Deleting document: {}", id);
        
        ragApplicationService.deleteDocument(id);
        
        return ResponseEntity.noContent().build();
    }

    /**
     * Streaming RAG chat endpoint using Server-Sent Events.
     * Sends sources AFTER all chunks are streamed.
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "RAG streaming chat", description = "Streaming RAG chat with source documents")
    public Flux<ServerSentEvent<String>> ragChatStream(@Valid @RequestBody RagChatRequest request) {
        log.info("RAG chat request: {} with docIds: {}", truncate(request.question()), request.docIds());

        try {
            // Convert string doc IDs to UUIDs
            List<UUID> docUuids = request.docIds() != null && !request.docIds().isEmpty()
                ? request.docIds().stream().map(UUID::fromString).toList()
                : null;

            // Retrieve context from specified documents (or all if not specified)
            var result = ragApplicationService.retrieveContext(
                request.question(),
                docUuids,
                request.topK() != null ? request.topK() : 5
            );

            String context = result.context();
            List<SourceDocument> sources = result.sources();
            String prompt = buildPrompt(request.question(), context);

            // Stream chunks, then send sources at the end
            return streamChunks(prompt)
                    .concatWith(Flux.defer(() -> sendSources(sources)));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid argument in RAG chat", e);
            return Flux.error(e);
        } catch (Exception e) {
            log.error("Error in RAG chat", e);
            return Flux.error(e);
        }
    }

    private Flux<ServerSentEvent<String>> streamChunks(String prompt) {
        record ChunkEvent(String type, String text) {}
        return aiChatService.chatStream(prompt)
                .map(text -> {
                    try {
                        String json = objectMapper.writeValueAsString(new ChunkEvent("chunk", text));
                        return ServerSentEvent.<String>builder().data(json).build();
                    } catch (JsonProcessingException e) {
                        log.error("Failed to serialize chunk event", e);
                        return ServerSentEvent.<String>builder()
                                .data("{\"type\":\"chunk\",\"text\":\"\"}")
                                .build();
                    }
                })
                .doOnError(e -> log.error("Stream error", e));
    }

    private Flux<ServerSentEvent<String>> sendSources(List<SourceDocument> sources) {
        List<SourceDocumentDto> sourceDtos = sources.stream()
                .map(s -> new SourceDocumentDto(null, s.text(), (float) s.score(), s.metadata(), s.index(), s.documentTitle()))
                .toList();
        return Flux.just(sourceDtos)
                .map(dto -> ServerSentEvent.<String>builder()
                        .data(toJson(new SourcesEvent(sourceDtos)))
                        .event("sources")
                        .build())
                .doOnError(e -> log.error("Sources error", e));
    }

    private record SourcesEvent(List<SourceDocumentDto> sources) {}

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("JSON serialization error", e);
            return "{}";
        }
    }

    private String buildPrompt(String question, String context) {
        String languageCode = languageDetectionService.detect(question);
        return languageDetectionService.buildPrompt(question, context, languageCode);
    }

    private DocumentSummaryDto toDocumentSummaryDto(Document document) {
        return new DocumentSummaryDto(
            document.getId().value(),
            document.getTitle(),
            document.getStatus().name(),
            document.getCreatedAt(),
            0
        );
    }

    private String truncate(String text) {
        if (text == null) return "null";
        if (text.length() <= 50) return text;
        return text.substring(0, 50) + "...";
    }
}
