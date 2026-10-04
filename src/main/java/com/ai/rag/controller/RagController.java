package com.ai.rag.controller;

import com.ai.account.controller.OwnerContext;
import com.ai.rag.controller.dto.DocumentListResponse;
import com.ai.rag.controller.dto.DocumentSummaryResponse;
import com.ai.rag.controller.dto.RagChatRequest;
import com.ai.rag.controller.dto.UploadDocumentResponse;
import com.ai.rag.domain.model.RagDocument;
import com.ai.rag.domain.vo.DocumentId;
import com.ai.rag.service.DocumentUploadService;
import com.ai.rag.service.RagApplicationService;
import com.ai.rag.service.RagChatService;
import com.ai.vision.service.VisionChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
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
  private final RagChatService ragChatService;
  private final ObjectProvider<VisionChatService> visionChatService;

  public RagController(
      RagApplicationService ragApplicationService,
      RagChatService ragChatService,
      ObjectProvider<VisionChatService> visionChatService,
      OwnerContext ownerContext) {
    this.ownerContext = ownerContext;
    this.ragApplicationService = ragApplicationService;
    this.ragChatService = ragChatService;
    this.visionChatService = visionChatService;
  }

  @GetMapping("/documents")
  @Operation(summary = "List documents for the current owner")
  public ResponseEntity<DocumentListResponse> listDocuments(HttpServletRequest request) {
    String ownerKey = ownerContext.requireValue(request);
    List<RagDocument> documents = ragApplicationService.listDocuments(ownerKey);
    Map<DocumentId, Integer> chunkCounts = ragApplicationService.chunkCounts(documents);
    return ResponseEntity.ok(
        new DocumentListResponse(
            documents.stream().map(doc -> toSummary(doc, chunkCounts)).toList()));
  }

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
                result.documentId().uuidValue(),
                result.title(),
                result.status(),
                result.chunkCount(),
                null));
  }

  @DeleteMapping("/documents/{id}")
  @Operation(summary = "Delete a document")
  public ResponseEntity<Void> deleteDocument(@PathVariable UUID id, HttpServletRequest request) {
    ragApplicationService.deleteDocument(id, ownerContext.requireValue(request));
    return ResponseEntity.noContent().build();
  }

  @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @Operation(summary = "RAG streaming chat")
  public Flux<ServerSentEvent<String>> ragChatStream(@Valid @RequestBody RagChatRequest request) {
    if (!request.images().isEmpty()) {
      VisionChatService visionChat = visionChatService.getIfAvailable();
      if (visionChat == null) {
        return ragChatService.chatStream(
            request.question(), request.documentIds(), request.topK(), request.sessionId());
      }
      return visionChat.chatStreamWithImages(
          request.question(), request.documentIds(), request.images(), request.topK());
    }
    return ragChatService.chatStream(
        request.question(), request.documentIds(), request.topK(), request.sessionId());
  }

  private DocumentSummaryResponse toSummary(RagDocument doc, Map<DocumentId, Integer> chunkCounts) {
    return new DocumentSummaryResponse(
        doc.getId().uuidValue(),
        doc.getTitle(),
        doc.getStatus().name(),
        doc.getCreatedAt(),
        chunkCounts.getOrDefault(doc.getId(), 0));
  }
}
