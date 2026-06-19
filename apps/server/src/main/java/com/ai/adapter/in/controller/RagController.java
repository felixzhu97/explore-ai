package com.ai.adapter.in.controller;

import com.ai.domain.service.LanguageDetectionService;
import com.ai.domain.service.PromptTemplates;
import com.ai.domain.model.Document;
import com.ai.domain.model.SourceDocument;
import com.ai.domain.service.AiChatService;
import com.ai.domain.service.RagService;
import com.ai.adapter.out.document.PdfTextExtractor;
import com.ai.adapter.in.dto.*;
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
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/rag")
@Tag(name = "RAG", description = "RAG document management and chat")
public class RagController {

    private final RagService ragService;
    private final LanguageDetectionService languageDetectionService;
    private final PromptTemplates promptTemplates;
    private final AiChatService aiChatService;
    private final ObjectMapper objectMapper;
    private final PdfTextExtractor pdfTextExtractor;

    public RagController(RagService ragService,
                         LanguageDetectionService languageDetectionService,
                         PromptTemplates promptTemplates,
                         AiChatService aiChatService,
                         ObjectMapper objectMapper,
                         PdfTextExtractor pdfTextExtractor) {
        this.ragService = ragService;
        this.languageDetectionService = languageDetectionService;
        this.promptTemplates = promptTemplates;
        this.aiChatService = aiChatService;
        this.objectMapper = objectMapper;
        this.pdfTextExtractor = pdfTextExtractor;
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
        String fileName = file.getOriginalFilename();
        String docTitle = title != null ? title : fileName;

        String content;
        try {
            byte[] fileBytes = file.getBytes();
            String extension = pdfTextExtractor.getExtension(fileName);

            if ("pdf".equalsIgnoreCase(extension)) {
                var extractedText = pdfTextExtractor.extractText(fileBytes);
                if (extractedText.isEmpty()) {
                    throw new RuntimeException("Failed to extract text from PDF file: " + fileName);
                }
                content = extractedText.get();
                log.info("Extracted {} characters from PDF: {}", content.length(), fileName);
            } else {
                content = new String(fileBytes, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.error("Failed to read file content", e);
            throw new RuntimeException("Failed to read file content: " + e.getMessage(), e);
        }

        log.info("Uploading document: {}", docTitle);
        Document document = ragService.uploadDocument(docTitle, fileName, file.getSize(), content);

        UploadDocumentResponse response = new UploadDocumentResponse(
            document.getId().value(),
            document.getTitle(),
            document.getStatus().name(),
            0,
            document.getCreatedAt()
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
        log.info("RAG chat request: {} with docIds: {}", truncate(request.question()), request.docIds());

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

            return streamResponseReactive(aiResponse, sources);

        } catch (Exception e) {
            log.error("Error in RAG chat", e);
            return Flux.error(e);
        }
    }

    private Flux<ServerSentEvent<String>> streamResponseReactive(String prompt, List<SourceDocument> sources) {
        String[] words = prompt.split(" ");
        return Flux.fromArray(words)
                .delayElements(Duration.ofMillis(30))
                .map(word -> ServerSentEvent.<String>builder().data(word + " ").build())
                .concatWith(Flux.defer(() -> {
                    try {
                        List<SourceDocumentDto> sourceDtos = sources.stream()
                                .map(this::toSourceDocumentDto)
                                .collect(Collectors.toList());
                        String sourcesJson = objectMapper.writeValueAsString(sourceDtos);
                        return Flux.just(ServerSentEvent.<String>builder()
                                .event("sources")
                                .data(sourcesJson)
                                .build());
                    } catch (JsonProcessingException e) {
                        log.error("Error serializing sources", e);
                        return Flux.empty();
                    }
                }));
    }

    private String buildPrompt(String question, String context) {
        String languageCode = languageDetectionService.detect(question);
        String basePrompt = languageDetectionService.buildPrompt(question, context, languageCode);
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
