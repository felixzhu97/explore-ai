package com.ai.rag.controller;

import com.ai.account.controller.OwnerContext;
import com.ai.rag.controller.dto.DocumentListResponse;
import com.ai.rag.controller.dto.DocumentSummaryDto;
import com.ai.rag.controller.dto.RagChatRequest;
import com.ai.rag.controller.dto.UploadDocumentResponse;
import com.ai.rag.domain.model.Document;
import com.ai.rag.service.usecase.DocumentUploadService;
import com.ai.rag.service.usecase.RagApplicationService;
import com.ai.rag.service.usecase.RagChatUseCase;
import com.ai.rag.service.usecase.VisionChatUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

/** REST Controller for RAG operations. Thin controller - delegates to application services. */
@RestController
@RequestMapping("/api/rag")
@Tag(name = "RAG", description = "RAG document management and chat")
public class RagController {

  private final OwnerContext ownerContext;

  private final RagApplicationService ragApplicationService;
  private final RagChatUseCase ragChatUseCase;
  private final ObjectProvider<VisionChatUseCase> visionChatUseCase;

  /** Documentation. */
  public RagController(
      RagApplicationService ragApplicationService,
      RagChatUseCase ragChatUseCase,
      ObjectProvider<VisionChatUseCase> visionChatUseCase,
      OwnerContext ownerContext) {
    this.ownerContext = ownerContext;
    this.ragApplicationService = ragApplicationService;
    this.ragChatUseCase = ragChatUseCase;
    this.visionChatUseCase = visionChatUseCase;
  }

  /** Documentation. */
  @GetMapping("/documents")
  @Operation(summary = "List documents for the current owner")
  public ResponseEntity<DocumentListResponse> listDocuments(HttpServletRequest request) {
    String ownerKey = ownerContext.requireValue(request);
    return ResponseEntity.ok(
        new DocumentListResponse(
            ragApplicationService.listDocuments(ownerKey).stream().map(this::toSummary).toList()));
  }

  /** Documentation. */
  @PostMapping("/documents/upload")
  @Operation(summary = "Upload a document")
  public ResponseEntity<UploadDocumentResponse> uploadDocument(
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "title", required = false) String title,
      HttpServletRequest request) {
    String ownerKey = ownerContext.requireValue(request);
    DocumentUploadService.UploadResult result =
        ragApplicationService.uploadDocument(file, title, ownerKey);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new UploadDocumentResponse(
                result.documentId().value(),
                result.title(),
                result.status(),
                result.chunkCount(),
                null));
  }

  /** Documentation. */
  @DeleteMapping("/documents/{id}")
  @Operation(summary = "Delete a document")
  public ResponseEntity<Void> deleteDocument(@PathVariable UUID id, HttpServletRequest request) {
    ragApplicationService.deleteDocument(id, ownerContext.requireValue(request));
    return ResponseEntity.noContent().build();
  }

  /** Documentation. */
  @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @Operation(summary = "RAG streaming chat")
  public Flux<ServerSentEvent<String>> ragChatStream(@Valid @RequestBody RagChatRequest request) {
    if (hasImages(request.images())) {
      VisionChatUseCase visionChat = visionChatUseCase.getIfAvailable();
      if (visionChat == null) {
        return ragChatUseCase.chatStream(
            request.question(), request.docIds(), request.topK(), request.sessionId());
      }
      return visionChat.chatStreamWithImages(
          request.question(), request.docIds(), request.images(), request.topK());
    }
    return ragChatUseCase.chatStream(
        request.question(), request.docIds(), request.topK(), request.sessionId());
  }

  private boolean hasImages(List<String> images) {
    return images != null && !images.isEmpty();
  }

  private DocumentSummaryDto toSummary(Document doc) {
    return new DocumentSummaryDto(
        doc.getId().value(), doc.getTitle(), doc.getStatus().name(), doc.getCreatedAt(), 0);
  }
}
