package com.ai.interfaces.controller;

import com.ai.application.service.LanguageDetectionService;
import com.ai.application.service.RagApplicationService;
import com.ai.domain.model.Document;
import com.ai.domain.model.SourceDocument;
import com.ai.domain.service.AiChatService;
import com.ai.infrastructure.adapter.document.PdfTextExtractor;
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
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "RAG streaming chat", description = "Streaming RAG chat with source documents")
    public Flux<ServerSentEvent<String>> ragChatStream(@Valid @RequestBody RagChatRequest request) {
        log.info("RAG chat request: {} with docIds: {}", truncate(request.question()), request.docIds());

        try {
            // Convert string doc IDs to UUIDs
            List<UUID> docUuids = null;
            if (request.docIds() != null && !request.docIds().isEmpty()) {
                docUuids = request.docIds().stream()
                    .map(UUID::fromString)
                    .collect(Collectors.toList());
            }

            // Retrieve context from specified documents (or all if not specified)
            var result = ragApplicationService.retrieveContext(
                request.question(),
                docUuids,
                request.topK() != null ? request.topK() : 5
            );

            String context = result.context();
            List<SourceDocument> sources = result.sources();

            // Build the prompt with context
            String prompt = buildPrompt(request.question(), context);

            // Stream the AI response using real ChatClient streaming
            return streamResponseReactive(prompt, sources);

        } catch (Exception e) {
            log.error("Error in RAG chat", e);
            return Flux.error(e);
        }
    }

    private Flux<ServerSentEvent<String>> streamResponseReactive(String prompt,
                                                                List<SourceDocument> sources) {
        try {
            // Step 1: Build sources JSON and emit as SSE event
            List<SourceDocumentDto> sourceDtos = sources.stream()
                    .map(this::toSourceDocumentDto)
                    .collect(Collectors.toList());
            String sourcesJson = objectMapper.writeValueAsString(
                    new SourcesWrapper(sourceDtos)
            );

            // Step 2: Get real streaming AI response
            Flux<String> aiStream = aiChatService.chatStream(prompt);

            // Emit sources first, then stream AI chunks
            return Flux.concat(
                    Flux.just(sourcesJson),
                    aiStream.map(text -> {
                        try {
                            return objectMapper.writeValueAsString(new ChunkData("chunk", text));
                        } catch (JsonProcessingException e) {
                            log.error("Error serializing chunk", e);
                            return "";
                        }
                    })
            ).filter(json -> !json.isEmpty())
                    .map(json -> ServerSentEvent.<String>builder()
                            .data(json)
                            .build());
        } catch (JsonProcessingException e) {
            log.error("Error serializing stream data", e);
            return Flux.error(e);
        }
    }

    private record ChunkData(String type, String text) {}
    private record SourcesWrapper(List<SourceDocumentDto> sources) {}

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
            0 // chunkCount not available from Document model
        );
    }

    private SourceDocumentDto toSourceDocumentDto(SourceDocument source) {
        return new SourceDocumentDto(
            null, // id not available in SourceDocument
            source.text(),
            (float) source.score(),
            source.metadata()
        );
    }

    private String truncate(String text) {
        if (text == null) return "null";
        if (text.length() <= 50) return text;
        return text.substring(0, 50) + "...";
    }
}
