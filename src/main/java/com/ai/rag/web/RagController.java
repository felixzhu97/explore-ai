package com.ai.rag.web;

import com.ai.ai.infrastructure.streaming.StreamingService;
import com.ai.rag.domain.model.Document;
import com.ai.rag.domain.model.SourceDocument;
import com.ai.rag.application.usecase.RagApplicationService;
import com.ai.rag.application.usecase.DocumentUploadUseCase;
import com.ai.rag.application.usecase.RagChatUseCase;
import com.ai.rag.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for RAG operations.
 * Thin controller - handles HTTP concerns only, delegates business logic to application services.
 */
@RestController
@RequestMapping("/api/rag")
@Tag(name = "RAG", description = "RAG document management and chat")
public class RagController {

    private static final Logger log = LoggerFactory.getLogger(RagController.class);

    private final RagApplicationService ragApplicationService;
    private final DocumentUploadUseCase documentUploadUseCase;
    private final RagChatUseCase ragChatUseCase;
    private final StreamingService streamingService;

    public RagController(
            RagApplicationService ragApplicationService,
            DocumentUploadUseCase documentUploadUseCase,
            RagChatUseCase ragChatUseCase,
            StreamingService streamingService) {
        this.ragApplicationService = ragApplicationService;
        this.documentUploadUseCase = documentUploadUseCase;
        this.ragChatUseCase = ragChatUseCase;
        this.streamingService = streamingService;
    }

    @GetMapping("/documents/")
    @Operation(summary = "List all documents")
    public ResponseEntity<DocumentListResponse> listDocuments() {
        List<Document> documents = ragApplicationService.listDocuments();
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
        ragApplicationService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "RAG streaming chat")
    public Flux<ServerSentEvent<String>> ragChatStream(@Valid @RequestBody RagChatRequest request) {
        try {
            var chatResult = ragChatUseCase.chat(request.question(), request.docIds(), request.topK());

            List<SourceDocumentDto> sourceDtos = chatResult.sources().stream()
                    .map(this::toSourceDocumentDto)
                    .collect(Collectors.toList());

            return streamingService.streamWithSources(chatResult.response(), sourceDtos);

        } catch (Exception e) {
            log.error("Error in RAG chat", e);
            return Flux.error(e);
        }
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
}
