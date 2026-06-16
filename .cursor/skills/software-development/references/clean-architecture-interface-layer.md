# Clean Architecture — Interface Adapters Layer

The Interface Adapters layer (also called Interface layer) is the outermost layer that sits at the boundary between external systems and the application. It is responsible for translating data into and out of the application, handling HTTP requests and responses, and converting between external formats and the application's internal representations.

## Layer Responsibilities

The Interface layer is responsible for:

- **Receiving incoming requests** from external clients (HTTP, CLI, message queues)
- **Validating input data** and transforming it into application commands
- **Invoking application layer services** with the appropriate input
- **Transforming application results** into response formats
- **Handling exceptions** and converting them to appropriate error responses
- **Managing HTTP concerns** like headers, content types, and status codes

The Interface layer depends on the Application layer but knows nothing about the Domain layer directly. It should never instantiate domain objects or call domain methods directly; all interactions flow through application use cases and services.

## Allowed / Forbidden Dependencies

### Allowed Dependencies

- Application layer classes (`com.ai.application.*`)
- Domain layer classes (for exception handling and type references)
- HTTP framework classes (`jakarta.servlet.*`, `org.springframework.web.*`)
- DTO classes defined in the Interface layer
- Mapping/transformation libraries
- Validation annotations (`jakarta.validation.*`)

### Forbidden Dependencies

- Infrastructure layer classes (`com.ai.infrastructure.*`) in controllers
- Direct instantiation of infrastructure adapters
- Business logic or domain rule implementation
- Database connections or transaction management
- External service clients (these belong in Infrastructure)

The Interface layer is the entry point for external requests. It should orchestrate calls to the Application layer but not implement any business logic.

## Skeleton (Directory Layout)

```
src/main/java/com/ai/interfaces/
    controller/              # REST controllers (request handlers)
        RagController.java
        ChatController.java
        GlobalExceptionHandler.java
    dto/                     # Request and response DTOs
        request/
            UploadDocumentRequest.java
            RagChatRequest.java
            ChatRequest.java
        response/
            UploadDocumentResponse.java
            RagChatResponse.java
            DocumentSummaryDto.java
            SourceDocumentDto.java
            MessageHistoryResponse.java
    mapper/                  # Interface mappers (if needed for complex transformations)
        # DtoMapper.java
```

## Code Patterns

### REST Controller

Controllers handle HTTP requests and responses. They should be thin — their primary responsibilities are request parsing, input validation, calling application services, and response formatting.

#### Good (Java)

```java
package com.ai.interfaces.controller;

import com.ai.application.service.LanguageDetectionService;
import com.ai.application.service.RagApplicationService;
import com.ai.domain.model.Document;
import com.ai.domain.model.SourceDocument;
import com.ai.domain.service.AiChatService;
import com.ai.infrastructure.adapter.document.PdfTextExtractor;
import com.ai.interfaces.dto.*;
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
    private final PdfTextExtractor pdfTextExtractor;

    public RagController(
            RagApplicationService ragApplicationService,
            LanguageDetectionService languageDetectionService,
            AiChatService aiChatService,
            PdfTextExtractor pdfTextExtractor) {
        this.ragApplicationService = ragApplicationService;
        this.languageDetectionService = languageDetectionService;
        this.aiChatService = aiChatService;
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
                var extractedText = pdfTextExtractor.extractText(fileBytes);
                if (extractedText.isEmpty()) {
                    throw new RuntimeException("Failed to extract text from PDF: " + fileName);
                }
                content = extractedText.get();
            } else {
                content = new String(fileBytes, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file content: " + e.getMessage(), e);
        }

        Document document = ragApplicationService.uploadDocument(
                docTitle, fileName, file.getSize(), content);

        UploadDocumentResponse response = new UploadDocumentResponse(
                document.getId().value(),
                document.getTitle(),
                document.getStatus().name(),
                0,
                document.getCreatedAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Streaming RAG chat endpoint.
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> ragChatStream(
            @Valid @RequestBody RagChatRequest request) {
        log.info("RAG chat request: {}", truncate(request.question()));

        List<UUID> docUuids = null;
        if (request.docIds() != null && !request.docIds().isEmpty()) {
            docUuids = request.docIds().stream()
                    .map(UUID::fromString)
                    .collect(Collectors.toList());
        }

        var result = ragApplicationService.retrieveContext(
                request.question(), docUuids,
                request.topK() != null ? request.topK() : 5);

        String context = result.context();
        List<SourceDocument> sources = result.sources();
        String prompt = languageDetectionService.buildPrompt(
                request.question(), context,
                languageDetectionService.detect(request.question()));

        return streamResponse(prompt, sources);
    }

    // Response mapping helpers
    private DocumentSummaryDto toDocumentSummaryDto(Document document) {
        return new DocumentSummaryDto(
                document.getId().value(),
                document.getTitle(),
                document.getStatus().name(),
                document.getCreatedAt(),
                0
        );
    }

    private Flux<ServerSentEvent<String>> streamResponse(String prompt,
            List<SourceDocument> sources) {
        return aiChatService.chatStream(prompt)
                .map(word -> ServerSentEvent.<String>builder()
                        .data(word)
                        .build())
                .concatWith(sendSourcesEvent(sources))
                .doOnError(e -> log.error("Error in streaming", e));
    }

    private Flux<ServerSentEvent<String>> sendSourcesEvent(List<SourceDocument> sources) {
        // Send sources as final SSE event
        return Flux.empty();
    }

    private String truncate(String text) {
        if (text == null) return "null";
        if (text.length() <= 50) return text;
        return text.substring(0, 50) + "...";
    }
}
```

The controller is thin — it handles HTTP concerns, validates input, calls the application service, and transforms the response. The actual business logic resides in `RagApplicationService`.

### Request DTO

Request DTOs carry input data from clients. They should validate input using Bean Validation annotations.

#### Good (Java)

```java
package com.ai.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Upload document request DTO.
 * Carries file upload data from the client.
 */
public record UploadDocumentRequest(
        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Content is required")
        String content,

        String fileName
) {}
```

Using Java records for DTOs provides a concise syntax with immutable fields. The validation annotations are processed by Spring's `HandlerMethodArgumentResolver` before the controller method is invoked.

### Response DTO

Response DTOs carry output data to clients. They should be immutable and derived from domain or application objects.

#### Good (Java)

```java
package com.ai.interfaces.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Upload document response DTO.
 * Carries the result of document upload back to the client.
 */
public record UploadDocumentResponse(
        UUID documentId,
        String title,
        String status,
        int chunkCount,
        Instant createdAt
) {}

/**
 * Document summary for list responses.
 */
public record DocumentSummaryDto(
        UUID id,
        String title,
        String status,
        Instant createdAt,
        int chunkCount
) {}

/**
 * Source document reference from RAG retrieval.
 */
public record SourceDocumentDto(
        UUID id,
        String text,
        float score,
        java.util.Map<String, Object> metadata
) {}
```

Response DTOs are simple data carriers. When the response is more complex, consider using separate response records for different use cases.

### Global Exception Handler

Exception handlers convert domain and application exceptions into appropriate HTTP responses. They are the translation layer between application errors and external error formats.

#### Good (Java)

```java
package com.ai.interfaces.controller;

import com.ai.domain.exception.DocumentNotFoundException;
import com.ai.domain.exception.RagServiceException;
import com.ai.domain.model.AiServiceException;
import com.ai.domain.model.ChatSessionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler.
 * Converts domain exceptions to HTTP responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ChatSessionNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleSessionNotFound(
            ChatSessionNotFoundException e) {
        log.warn("Session not found: {}", e.getSessionId());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorResponse("SESSION_NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(AiServiceException.class)
    public ResponseEntity<Map<String, Object>> handleAiServiceError(AiServiceException e) {
        log.error("AI service error", e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(errorResponse("AI_SERVICE_ERROR", e.getMessage()));
    }

    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleDocumentNotFound(
            DocumentNotFoundException e) {
        log.warn("Document not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "error", "Document not found",
                        "documentId", e.getMessage().replace("Document not found: ", ""),
                        "type", "error",
                        "timestamp", Instant.now().toString()
                ));
    }

    @ExceptionHandler(RagServiceException.class)
    public ResponseEntity<Map<String, Object>> handleRagServiceError(RagServiceException e) {
        log.error("RAG service error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse("RAG_SERVICE_ERROR", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationError(
            MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation error: {}", message);
        return ResponseEntity.badRequest()
                .body(errorResponse("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSize(
            MaxUploadSizeExceededException e) {
        log.warn("Upload size exceeded: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(errorResponse("FILE_TOO_LARGE", "File exceeds maximum size of 50MB"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
    }

    private Map<String, Object> errorResponse(String type, String message) {
        return Map.of(
                "error", type,
                "message", message,
                "type", "error",
                "timestamp", Instant.now().toString()
        );
    }
}
```

Exception handlers translate domain exceptions into HTTP status codes and structured error responses. They do not implement business logic.

## Anti-Patterns Caught by This Layer

### Business Logic in Controller

#### Bad

```java
@RestController
public class BadRagController {

    @PostMapping("/documents")
    public ResponseEntity<Document> uploadDocument(@RequestBody UploadDocumentRequest req) {
        // Business logic belongs in Application layer, not here
        if (req.title().isBlank()) {
            throw new IllegalArgumentException("Title required");
        }

        Document document = new Document(DocumentId.generate(), req.title(), ...);
        document.markProcessing();  // Direct domain manipulation

        // Database access from controller
        documentRepository.save(document);

        // Direct vector store access
        for (String chunk : chunkText(req.content())) {
            vectorStore.save(embed(chunk));
        }

        return ResponseEntity.ok(document);
    }
}
```

#### Good

```java
@RestController
public class RagController {

    private final RagApplicationService ragApplicationService;

    @PostMapping("/documents")
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestBody UploadDocumentRequest req) {
        // Controller delegates to application service
        Document document = ragApplicationService.uploadDocument(
                req.title(), req.fileName(), req.fileSize(), req.content());

        return ResponseEntity.ok(toResponse(document));
    }
}
```

### DTO Passed Directly to Domain

#### Bad

```java
// Interface layer passes HTTP DTO directly to domain
public ResponseEntity<Void> createDocument(@RequestBody CreateDocumentDto dto) {
    // DTO has validation annotations (Hibernate Validator) leaking to domain
    domainService.process(dto);  // FORBIDDEN
}
```

#### Good

```java
// Interface layer transforms DTO to domain object or application command
public ResponseEntity<Void> createDocument(@RequestBody CreateDocumentDto dto) {
    // DTO is transformed into primitives before reaching domain
    Document document = Document.create(dto.title(), dto.fileName(), dto.fileSize());
    documentRepository.save(document);
}
```

## Real Reference in This Workspace

All interface layer code in this workspace is located at:

```
apps/server/src/main/java/com/ai/interfaces/
```

Key reference files demonstrating the patterns:

- `controller/RagController.java` — REST controller with streaming support
- `dto/UploadDocumentRequest.java` — request DTO with validation
- `controller/GlobalExceptionHandler.java` — exception handling
- `dto/RagChatRequest.java` — chat request DTO

## Verification (ArchUnit)

### ArchUnit Rule Example

```java
package com.ai.interfaces;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class InterfaceLayerArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter()
            .importPackages("com.ai.interfaces..");

    @Test
    void controllersMustNotDependOnInfrastructure() {
        ArchRule rule = noClasses()
                .that().haveNameMatching(".*Controller")
                .should().dependOnClassesThat()
                .resideInPackage("com.ai.infrastructure..");

        rule.check(classes);
    }

    @Test
    void controllersShouldBeAnnotatedWithRestController() {
        ArchRule rule = noClasses()
                .that().haveNameMatching(".*Controller")
                .should().beAnnotatedWith("org.springframework.web.bind.annotation.RestController");

        rule.check(classes);
    }

    @Test
    void exceptionHandlerShouldBeAnnotated() {
        ArchRule rule = noClasses()
                .that().haveNameMatching(".*ExceptionHandler")
                .should().beAnnotatedWith("org.springframework.web.bind.annotation.RestControllerAdvice");

        rule.check(classes);
    }
}
```

## TypeScript (Angular HTTP Interceptor)

For Angular applications, HTTP interceptors handle cross-cutting concerns at the HTTP layer:

```typescript
// Angular HTTP interceptor for error handling
import { HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';

export const errorHandlerInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(
    catchError((error) => {
      console.error('HTTP Error:', error);

      // Transform error to application error format
      const applicationError = {
        type: error.error?.error || 'INTERNAL_ERROR',
        message: error.error?.message || 'An unexpected error occurred',
        timestamp: new Date().toISOString()
      };

      return throwError(() => applicationError);
    })
  );
};
```

---

## Cross-References

- [`../code-quality.md`](../code-quality.md) — Quality gates and architecture verification
- [`../clean-code-naming.md`](../clean-code-naming.md) — Naming conventions for DTOs and controllers
- [`../clean-code-functions.md`](../clean-code-functions.md) — Controller method design
- [`../clean-code-error-handling.md`](../clean-code-error-handling.md) — Error response patterns
- [`../clean-code-formatting.md`](../clean-code-formatting.md) — Response formatting
- [`../clean-code-testing.md`](../clean-code-testing.md) — Controller testing
- [`../../software-architecture/SKILL.md`](../../software-architecture/SKILL.md) — Software architecture methodology

## Based on `apps/server` current structure (commit `e251a5b2`)