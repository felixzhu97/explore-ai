# Clean Architecture — End-to-End Walkthrough

This document traces a single use case — document upload with RAG processing — through all four layers of Clean Architecture, showing how data flows and where transformations occur. It then contrasts the correct implementation with common anti-patterns.

## Use Case: Document Upload with RAG Processing

The user uploads a PDF document. The system extracts text, chunks it, generates embeddings, stores them in a vector database, and returns confirmation.

### Layer-by-Layer Flow

```
Interface Layer         Application Layer        Domain Layer           Infrastructure Layer
     |                        |                      |                        |
     |  HTTP POST /upload     |                      |                        |
     |  multipart/form-data   |                      |                        |
     |----------------------->|                      |                        |
     |                        |                      |                        |
     |  [Parse file]          |                      |                        |
     |  [Extract text]         |                      |                        |
     |                        |                      |                        |
     |  UploadDocumentRequest |                      |                        |
     |  title, content        |                      |                        |
     |----------------------->|                      |                        |
     |                        |                      |                        |
     |                        |  UploadDocumentUseCase|                        |
     |                        |  .execute(title, ...) |                        |
     |                        |--------------------->|                        |
     |                        |                      |                        |
     |                        |                      |  new Document(...)      |
     |                        |                      |  document.markReady()   |
     |                        |                      |<-----------------------||
     |                        |                      |                        |
     |                        |  DocumentRepositoryPort|                       |
     |                        |  .save(document)     |                        |
     |                        |-----------------------|------------------------>|
     |                        |                      |                        |  JpaDocumentRepository
     |                        |                      |                        |  toEntity() -> save()
     |                        |                      |                        |----------------------|
     |                        |                      |                        |  [PostgreSQL INSERT]  |
     |                        |                      |                        |----------------------|
     |                        |                      |                        |  toDomain() <- entity|
     |                        |                      |                        |----------------------|
     |                        |  Document            |                        |
     |                        |<----------------------|------------------------|
     |                        |                      |                        |
     |                        |  EmbeddingPort        |                        |
     |                        |  .embed(chunk)       |                        |
     |                        |----------------------|------------------------>|
     |                        |                      |                        |  OllamaEmbeddingAdapter
     |                        |                      |                        |----------------------|
     |                        |                      |                        |  [HTTP POST /api/embed]|
     |                        |                      |                        |----------------------|
     |                        |  float[]             |                        |
     |                        |<----------------------|------------------------|
     |                        |                      |                        |
     |                        |  VectorSearchPort     |                        |
     |                        |  .saveChunk(chunk)   |                        |
     |                        |----------------------|------------------------>|
     |                        |                      |                        |  PgVectorAdapter
     |                        |                      |                        |----------------------|
     |                        |                      |                        |  [INSERT INTO vectors]|
     |                        |                      |                        |----------------------|
     |                        |                      |                        |
     |  DocumentResponse     |                      |                        |
     |<-----------------------|                      |                        |
     |                        |                      |                        |
     |  HTTP 201 Created      |                      |                        |
     |  {documentId, status}  |                      |                        |
     |<-----------------------|                      |                        |
```

### Step-by-Step Code

#### Step 1: Interface Layer — Controller Receives Request

```java
@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagApplicationService ragApplicationService;

    @PostMapping("/documents/upload")
    public ResponseEntity<UploadDocumentResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title) {

        String fileName = file.getOriginalFilename();
        String docTitle = title != null ? title : fileName;

        // File processing in controller (HTTP concern)
        String content;
        try {
            byte[] fileBytes = file.getBytes();
            String extension = getExtension(fileName);

            if ("pdf".equalsIgnoreCase(extension)) {
                var extractedText = pdfTextExtractor.extractText(fileBytes);
                content = extractedText.orElseThrow(
                    () -> new RuntimeException("Failed to extract text from PDF: " + fileName));
            } else {
                content = new String(fileBytes, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file content: " + e.getMessage(), e);
        }

        // Transform: Request -> Application layer call
        Document document = ragApplicationService.uploadDocument(
                docTitle, fileName, file.getSize(), content);

        // Transform: Domain object -> Response DTO
        UploadDocumentResponse response = new UploadDocumentResponse(
                document.getId().value(),
                document.getTitle(),
                document.getStatus().name(),
                0,
                document.getCreatedAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

**Data transformation**: `MultipartFile` (HTTP) -> `String content` (application input) -> `Document` (domain) -> `UploadDocumentResponse` (HTTP)

#### Step 2: Application Layer — Use Case Orchestration

```java
public class UploadDocumentUseCase {

    private final DocumentRepositoryPort documentRepository;
    private final EmbeddingPort embeddingPort;
    private final VectorSearchPort vectorSearchPort;

    @Transactional
    public Document execute(String title, String fileName, Long fileSize, String content) {
        log.info("Uploading document: {}", title);

        // Create domain object with factory method
        Document document = new Document(DocumentId.generate(), title, fileName, fileSize);
        document.markProcessing();
        document = documentRepository.save(document);
        documentRepository.flush();

        try {
            // Chunk the content
            List<String> chunks = chunkText(content);

            // Embed each chunk and store in vector database
            for (int i = 0; i < chunks.size(); i++) {
                String chunkText = chunks.get(i);
                float[] embedding = embeddingPort.embed(chunkText);

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("title", title);
                metadata.put("fileName", fileName);

                DocumentChunk chunk = new DocumentChunk(
                        UUID.randomUUID(),
                        document.getId().value(),
                        chunkText,
                        i,
                        metadata
                ).withEmbedding(embedding);

                vectorSearchPort.saveChunk(chunk);
            }

            // Update status via domain method
            document.markReady();
            document = documentRepository.save(document);
            return document;

        } catch (Exception e) {
            log.error("Failed to process document", e);
            document.markFailed();
            documentRepository.save(document);
            throw new RuntimeException("Failed to process document: " + e.getMessage(), e);
        }
    }
}
```

**Domain events**: `document.markProcessing()` and `document.markReady()` are called by the use case, not by the domain itself. The domain exposes the allowed state transitions; the use case decides when to invoke them.

#### Step 3: Domain Layer — Business Rules

```java
public class Document {
    private final DocumentId id;
    private String title;
    private String fileName;
    private Long fileSize;
    private DocumentStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    // Factory method
    public Document(DocumentId id, String title, String fileName, Long fileSize) {
        this.id = id;
        this.title = title;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.status = DocumentStatus.UPLOADING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Full constructor for repository reconstitution
    public Document(DocumentId id, String title, String fileName, Long fileSize,
                   DocumentStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Business methods control state transitions
    public void markProcessing() {
        this.status = DocumentStatus.PROCESSING;
        this.updatedAt = Instant.now();
    }

    public void markReady() {
        this.status = DocumentStatus.READY;
        this.updatedAt = Instant.now();
    }

    public void markFailed() {
        this.status = DocumentStatus.FAILED;
        this.updatedAt = Instant.now();
    }

    // Getters only — no public setters
    public DocumentId getId() { return id; }
    public DocumentStatus getStatus() { return status; }
    // ...
}
```

**Encapsulation**: State transitions go through business methods. The domain enforces its own invariants.

#### Step 4: Infrastructure Layer — Persistence

```java
@Component
public class JpaDocumentRepository implements DocumentRepositoryPort {

    private final SpringDataDocumentRepository documentRepository;

    @Override
    @Transactional
    public Document save(Document document) {
        DocumentEntity entity = toEntity(document);
        DocumentEntity saved = documentRepository.save(entity);
        return toDomain(saved);
    }

    // Domain -> Entity transformation
    private DocumentEntity toEntity(Document document) {
        DocumentEntity.DocumentStatus status;
        switch (document.getStatus()) {
            case UPLOADING -> status = DocumentEntity.DocumentStatus.UPLOADING;
            case PROCESSING -> status = DocumentEntity.DocumentStatus.PROCESSING;
            case READY -> status = DocumentEntity.DocumentStatus.READY;
            case FAILED -> status = DocumentEntity.DocumentStatus.FAILED;
        }

        return new DocumentEntity(
                document.getId().value(),
                document.getTitle(),
                document.getFileName(),
                document.getFileSize(),
                status,
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }

    // Entity -> Domain transformation
    private Document toDomain(DocumentEntity entity) {
        DocumentStatus status = switch (entity.getStatus()) {
            case UPLOADING -> DocumentStatus.UPLOADING;
            case PROCESSING -> DocumentStatus.PROCESSING;
            case READY -> DocumentStatus.READY;
            case FAILED -> DocumentStatus.FAILED;
        };

        return new Document(
                DocumentId.of(entity.getId()),
                entity.getTitle(),
                entity.getFileName(),
                entity.getFileSize(),
                status,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
```

**Data transformation**: `Document` (domain) <-> `DocumentEntity` (JPA). The repository handles bidirectional mapping.

#### Step 5: Infrastructure Layer — External Service Adapter

```java
@Component
public class OllamaEmbeddingAdapter implements EmbeddingPort {

    private final EmbeddingModel embeddingModel;

    public OllamaEmbeddingAdapter(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Override
    public float[] embed(String text) {
        // Call external Ollama service
        var embeddings = embeddingModel.embed(text);

        // Convert to primitive array
        float[] result = new float[embeddings.size()];
        for (int i = 0; i < embeddings.size(); i++) {
            result[i] = embeddings.get(i).floatValue();
        }
        return result;
    }
}
```

**Dependency inversion**: `OllamaEmbeddingAdapter` implements `EmbeddingPort` defined in the Application layer. The application does not depend on Ollama directly.

---

## Anti-Pattern Contrast: Same Use Case Done Badly

### Anti-Pattern 1: Service-Controlled Architecture

In this anti-pattern, business logic resides in an application service instead of domain objects.

#### Bad

```java
public class BadRagApplicationService {

    @Transactional
    public Document uploadDocument(String title, String fileName, Long fileSize, String content) {
        // Business logic in service — violates rich domain model principle
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }

        if (fileSize > 50 * 1024 * 1024) {
            throw new IllegalArgumentException("File too large");
        }

        // Direct field manipulation — no business methods
        Document document = new Document(DocumentId.generate(), title, fileName, fileSize);
        document.status = DocumentStatus.PROCESSING;  // Direct field access!
        document.updatedAt = Instant.now();  // Direct field access!

        return documentRepository.save(document);
    }
}
```

#### Why It Is Bad

- Business rules are duplicated if the same validation appears elsewhere
- State transition rules are not encapsulated in the domain
- Testing requires mocking the entire service for business logic
- Business rules cannot be reused by other use cases

#### Good

```java
public class Document {
    // Business methods encapsulate state transitions
    public void markProcessing() {
        if (this.status != DocumentStatus.UPLOADING) {
            throw new IllegalStateException("Can only mark as processing from uploading state");
        }
        this.status = DocumentStatus.PROCESSING;
        this.updatedAt = Instant.now();
    }
}

public class UploadDocumentUseCase {
    // Use case orchestrates, domain encapsulates business rules
    public Document execute(...) {
        Document document = new Document(...);
        document.markProcessing();  // Domain enforces its own rules
        return documentRepository.save(document);
    }
}
```

### Anti-Pattern 2: JPA Entity as DTO

In this anti-pattern, JPA entities are passed through all layers instead of being mapped to domain objects.

#### Bad

```java
@Entity
@Table(name = "documents")
public class DocumentEntity {  // JPA entity leaked to all layers
    @Id
    private UUID id;
    private String title;
    private String fileName;
    @Enumerated(EnumType.STRING)
    private String status;
    // ...
}

@RestController
public class BadRagController {
    @PostMapping("/documents")
    public ResponseEntity<DocumentEntity> uploadDocument(@RequestBody DocumentEntity entity) {
        // JPA entity used as request DTO — validation annotations leak
        documentRepository.save(entity);  // JPA entity saved directly
        return ResponseEntity.ok(entity);  // JPA entity returned as response
    }
}
```

#### Why It Is Bad

- JPA annotations appear in HTTP contracts
- Domain logic cannot be tested without database
- Changing persistence technology breaks all layers
- Validation rules are tied to persistence concerns

#### Good

```java
// Domain entity (no JPA annotations)
public class Document { /* ... */ }

// Interface layer DTO
public record UploadDocumentRequest(
    @NotBlank String title,
    @NotBlank String content
) {}

// Controller uses DTO
@RestController
public class RagController {
    public ResponseEntity<DocumentResponse> uploadDocument(
            @Valid @RequestBody UploadDocumentRequest request) {
        Document document = ragApplicationService.uploadDocument(...);
        return ResponseEntity.ok(DocumentResponse.from(document));
    }
}

// Infrastructure maps between domain and JPA
@Component
public class JpaDocumentRepository implements DocumentRepositoryPort {
    public Document save(Document document) {
        DocumentEntity entity = toEntity(document);
        return toDomain(documentRepository.save(entity));
    }
}
```

### Anti-Pattern 3: Domain-Inverted Dependency

In this anti-pattern, dependencies point outward instead of inward.

#### Bad

```java
// Domain layer depends on Infrastructure
package com.ai.domain.model;

import com.ai.infrastructure.adapter.ai.SpringAiChatService;  // FORBIDDEN!

public class Document {
    private final SpringAiChatService aiService;  // Domain depends on Infrastructure

    public void processWithAI() {
        String result = aiService.chat(this.content);  // Domain calls infrastructure
    }
}
```

#### Good

```java
// Domain layer defines interface
package com.ai.domain.service;

public interface AiChatService {
    String chat(String userMessage);
}

// Infrastructure layer implements interface
package com.ai.infrastructure.adapter.ai;

@Component
public class SpringAiChatService implements AiChatService {
    // Implementation
}

// Application layer uses domain interface
package com.ai.application.port;

public interface AiChatPort {
    String chat(String userMessage);
}

// Adapter bridges application port to domain service
@Component
public class SpringAiChatAdapter implements AiChatPort {
    private final AiChatService aiChatService;
    // ...
}
```

---

## Real Reference Files in This Workspace

The document upload use case is implemented across these files:

| Layer | File |
|-------|------|
| Interface | `interfaces/controller/RagController.java` |
| Interface | `interfaces/dto/UploadDocumentRequest.java` |
| Application | `application/usecase/UploadDocumentUseCase.java` |
| Application | `application/service/RagApplicationService.java` |
| Domain | `domain/model/Document.java` |
| Domain | `domain/vo/DocumentId.java` |
| Domain | `domain/repository/DocumentRepositoryPort.java` |
| Infrastructure | `infrastructure/adapter/persistence/JpaDocumentRepository.java` |
| Infrastructure | `infrastructure/adapter/persistence/DocumentEntity.java` |
| Infrastructure | `infrastructure/adapter/embedding/OllamaEmbeddingAdapter.java` |

---

## 4-Layer Audit Checklist

Use this checklist when auditing an existing use case implementation:

### Interface Layer

- [ ] Controller handles HTTP concerns only (parsing, headers, status codes)
- [ ] Request DTOs use validation annotations (`@NotBlank`, `@NotNull`, etc.)
- [ ] Response DTOs are separate from domain entities
- [ ] Global exception handler translates domain exceptions to HTTP responses
- [ ] No business logic in controller methods
- [ ] No imports from `com.ai.infrastructure.*`

### Application Layer

- [ ] Use cases are single-responsibility (one public method per use case)
- [ ] Use cases orchestrate domain objects, not implement business rules
- [ ] Port interfaces are defined in Application layer
- [ ] No imports from `com.ai.infrastructure.*` or `com.ai.interfaces.*`
- [ ] Transactions are demarcated at use case level (`@Transactional`)

### Domain Layer

- [ ] Entities have business methods, not just getters and setters
- [ ] State transitions are encapsulated in domain methods
- [ ] No Spring/JPA/Hibernate annotations
- [ ] No imports from infrastructure or interface packages
- [ ] Value objects are immutable
- [ ] Repository interfaces are defined in Domain layer
- [ ] Domain events are immutable records

### Infrastructure Layer

- [ ] JPA entities are separate from domain entities
- [ ] Repository implementations implement Domain/Application port interfaces
- [ ] External service adapters implement Application port interfaces
- [ ] No imports from `com.ai.interfaces.*`
- [ ] Bidirectional mapping between domain and persistence objects
- [ ] Configuration classes handle framework integration

### Dependency Direction

- [ ] All dependencies point inward toward Domain
- [ ] Domain knows nothing about outer layers
- [ ] Infrastructure implements interfaces defined by inner layers
- [ ] Interface calls Application, never Domain or Infrastructure directly

---

## Cross-References

- [`../code-quality.md`](../code-quality.md) — Quality gates and architecture verification
- [`../clean-code-naming.md`](../clean-code-naming.md) — Naming conventions across layers
- [`../clean-code-functions.md`](../clean-code-functions.md) — Function design principles
- [`../clean-code-error-handling.md`](../clean-code-error-handling.md) — Error handling across layers
- [`../clean-code-testing.md`](../clean-code-testing.md) — Testing across layers
- [`../clean-architecture-domain-layer.md`](./clean-architecture-domain-layer.md) — Domain layer patterns
- [`../clean-architecture-application-layer.md`](./clean-architecture-application-layer.md) — Application layer patterns
- [`../clean-architecture-infrastructure-layer.md`](./clean-architecture-infrastructure-layer.md) — Infrastructure layer patterns
- [`../clean-architecture-interface-layer.md`](./clean-architecture-interface-layer.md) — Interface layer patterns
- [`../../software-architecture/SKILL.md`](../../software-architecture/SKILL.md) — Software architecture methodology

## Based on `apps/server` current structure (commit `e251a5b2`)