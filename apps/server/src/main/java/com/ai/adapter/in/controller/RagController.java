package com.ai.adapter.in.controller;

import com.ai.adapter.out.streaming.StreamingService;
import com.ai.domain.model.Document;
import com.ai.domain.model.SourceDocument;
import com.ai.domain.service.AiChatService;
import com.ai.domain.service.LanguageDetectionService;
import com.ai.domain.service.PromptTemplates;
import com.ai.domain.service.RagService;
import com.ai.domain.usecase.DocumentUploadUseCase;
import com.ai.adapter.in.dto.*;
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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for RAG operations.
 * Thin controller - handles HTTP concerns only, delegates business logic to domain services/use cases.
 */
@Slf4j
@RestController
@RequestMapping("/api/rag")
@Tag(name = "RAG", description = "RAG document management and chat")
public class RagController {

    private final RagService ragService;
    private final AiChatService aiChatService;
    private final LanguageDetectionService languageDetectionService;
    private final PromptTemplates promptTemplates;
    private final DocumentUploadUseCase documentUploadUseCase;
    private final StreamingService streamingService;

    public RagController(
            RagService ragService,
            AiChatService aiChatService,
            LanguageDetectionService languageDetectionService,
            PromptTemplates promptTemplates,
            DocumentUploadUseCase documentUploadUseCase,
            StreamingService streamingService) {
        this.ragService = ragService;
        this.aiChatService = aiChatService;
        this.languageDetectionService = languageDetectionService;
        this.promptTemplates = promptTemplates;
        this.documentUploadUseCase = documentUploadUseCase;
        this.streamingService = streamingService;
    }

    @GetMapping("/documents/")
    @Operation(summary = "List all documents")
    public ResponseEntity<DocumentListResponse> listDocuments() {
        log.info("Listing all documents");
        List<Document> documents = ragService.listDocuments();
        List<DocumentSummaryDto> summaries = documents.stream()
            .map(this::toDocumentSummaryDto)
            .collect(Collectors.toList());
        return ResponseEntity.ok(new DocumentListResponse(summaries));
    }

    @PostMapping("/documents/upload")
    @Operation(summary = "Upload a document")
    public ResponseEntity<UploadDocumentResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title) {

        log.info("Uploading document: {}", file.getOriginalFilename());

        var result = documentUploadUseCase.upload(file, title);

        UploadDocumentResponse response = new UploadDocumentResponse(
            result.documentId().value(),
            result.title(),
            result.status(),
            result.chunkCount(),
            null
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/documents/{id}")
    @Operation(summary = "Delete a document")
    public ResponseEntity<Void> deleteDocument(@PathVariable UUID id) {
        log.info("Deleting document: {}", id);
        ragService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "RAG streaming chat")
    public Flux<ServerSentEvent<String>> ragChatStream(@Valid @RequestBody RagChatRequest request) {
        log.info("RAG chat request: {}", truncate(request.question()));

        try {
            List<UUID> docUuids = null;
            if (request.docIds() != null && !request.docIds().isEmpty()) {
                docUuids = request.docIds().stream()
                    .map(UUID::fromString)
                    .collect(Collectors.toList());
            }

            var result = ragService.retrieveContext(
                request.question(),
                docUuids,
                request.topK() != null ? request.topK() : 5
            );

            String context = result.context();
            List<SourceDocument> sources = result.sources();
            String prompt = buildPrompt(request.question(), context);
            String aiResponse = aiChatService.chat(prompt);

            List<SourceDocumentDto> sourceDtos = sources.stream()
                    .map(this::toSourceDocumentDto)
                    .collect(Collectors.toList());

            return streamingService.streamWithSources(aiResponse, sourceDtos);

        } catch (Exception e) {
            log.error("Error in RAG chat", e);
            return Flux.error(e);
        }
    }

    private String buildPrompt(String question, String context) {
        String languageCode = languageDetectionService.detect(question);
        languageDetectionService.buildPrompt(question, context, languageCode);
        return promptTemplates.buildQuestionAnswerPrompt(context, question);
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

    private SourceDocumentDto toSourceDocumentDto(SourceDocument source) {
        return new SourceDocumentDto(null, source.text(), (float) source.score(), source.metadata());
    }

    private String truncate(String text) {
        if (text == null) return "null";
        if (text.length() <= 50) return text;
        return text.substring(0, 50) + "...";
    }
}
