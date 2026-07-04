package com.ai.rag.web;

import com.ai.common.streaming.StreamingService;
import com.ai.rag.application.usecase.DocumentUploadService;
import com.ai.rag.application.usecase.RagApplicationService;
import com.ai.rag.application.usecase.RagChatUseCase;
import com.ai.rag.application.usecase.VisionChatUseCase;
import com.ai.rag.domain.model.Document;
import com.ai.rag.domain.model.SourceDocument;
import com.ai.rag.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for RAG operations.
 * Thin controller - delegates to application services.
 */
@RestController
@RequestMapping("/api/rag")
@Tag(name = "RAG", description = "RAG document management and chat")
public class RagController {

    private final RagApplicationService ragApplicationService;
    private final RagChatUseCase ragChatUseCase;
    private final VisionChatUseCase visionChatUseCase;
    private final StreamingService streamingService;

    public RagController(
            RagApplicationService ragApplicationService,
            RagChatUseCase ragChatUseCase,
            VisionChatUseCase visionChatUseCase,
            StreamingService streamingService) {
        this.ragApplicationService = ragApplicationService;
        this.ragChatUseCase = ragChatUseCase;
        this.visionChatUseCase = visionChatUseCase;
        this.streamingService = streamingService;
    }

    @GetMapping("/documents")
    @Operation(summary = "List all documents")
    public ResponseEntity<DocumentListResponse> listDocuments() {
        return ResponseEntity.ok(new DocumentListResponse(
                ragApplicationService.listDocuments().stream()
                        .map(this::toSummary)
                        .toList()));
    }

    @PostMapping("/documents/upload")
    @Operation(summary = "Upload a document")
    public ResponseEntity<UploadDocumentResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title) {
        DocumentUploadService.UploadResult result = ragApplicationService.uploadDocument(file, title);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UploadDocumentResponse(
                        result.documentId().value(), result.title(),
                        result.status(), result.chunkCount(), null));
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
        RagChatResult result;
        if (hasImages(request.images())) {
            var visionResult = visionChatUseCase.chatWithImages(request.question(), request.docIds(), request.images(), request.topK());
            result = new RagChatResult(visionResult.response(), visionResult.sources());
        } else {
            var chatResult = ragChatUseCase.chat(request.question(), request.docIds(), request.topK());
            result = new RagChatResult(chatResult.response(), chatResult.sources());
        }

        var sourceDtos = result.sources().stream()
                .filter(s -> s.text() != null && !s.text().isBlank())
                .map(s -> new SourceDocumentDto(null, s.text(), (float) s.score(), s.metadata()))
                .toList();

        return streamingService.streamWithSources(result.response(), sourceDtos);
    }

    private boolean hasImages(List<String> images) {
        return images != null && !images.isEmpty();
    }

    private record RagChatResult(String response, List<SourceDocument> sources) {}

    private DocumentSummaryDto toSummary(Document doc) {
        return new DocumentSummaryDto(
                doc.getId().value(), doc.getTitle(),
                doc.getStatus().name(), doc.getCreatedAt(), 0);
    }
}
