# Clean Architecture — Application Layer

The Application layer sits between the Domain layer and the Interface layer. It orchestrates the flow of data and directs the Domain layer to use its business rules to achieve the use cases defined by the business. The Application layer is thin by design — it contains no business logic itself, only coordination logic.

## Layer Responsibilities

The Application layer is responsible for:

- **Orchestrating use cases** by coordinating domain objects and other application services
- **Defining port interfaces** that express the application's needs from external services
- **Transforming data** between interface DTOs and domain objects
- **Managing transactions** boundaries for operations that span multiple domain objects
- **Publishing application events** that notify external systems of completed operations

The Application layer depends only on the Domain layer. It knows nothing about HTTP requests, database connections, or external API clients. This separation allows the same use cases to be triggered by different interface adapters (REST, GraphQL, CLI) without modification.

## Allowed / Forbidden Dependencies

### Allowed Dependencies

- Domain layer classes (`com.ai.domain.*`)
- Application layer classes within the same bounded context
- Java standard library
- Logging framework (SLF4J)
- Spring's `@Transactional` annotation for transaction demarcation

### Forbidden Dependencies

The following are prohibited in the Application layer:

- Infrastructure layer packages (`com.ai.infrastructure.*`)
- Interface layer packages (`com.ai.interfaces.*`)
- JPA entities or Spring Data repositories
- HTTP servlet classes (`jakarta.servlet.*`, `javax.servlet.*`)
- Concrete implementations of port interfaces (only interfaces, not implementations)

Dependency direction must always point inward toward the Domain layer. The Application layer is the last layer that can depend on Domain; nothing outside of it should reach into Domain directly.

## Skeleton (Directory Layout)

```
src/main/java/com/ai/application/
    usecase/                 # Individual use case implementations
        UploadDocumentUseCase.java
        DeleteDocumentUseCase.java
        RagChatUseCase.java
        SendChatMessageUseCase.java
    port/                    # Port interfaces (application defines, infrastructure implements)
        AiChatPort.java
        DocumentRepositoryPort.java
        ChatSessionRepositoryPort.java
        EmbeddingPort.java
        VectorSearchPort.java
    service/                 # Application services (orchestration)
        RagApplicationService.java
        ChatApplicationService.java
        LanguageDetectionService.java
    dto/                     # Application-level DTOs (Command/Query/Result)
        # Commands, queries, and results specific to this layer
```

## Code Patterns

### Use Case (Command Pattern)

A use case represents a single action the system can perform. Use cases receive commands (input data), execute domain logic, and return results. They are typically stateless and can be composed into larger workflows.

#### Good (Java)

```java
package com.ai.application.usecase;

import com.ai.application.port.DocumentRepositoryPort;
import com.ai.application.port.EmbeddingPort;
import com.ai.application.port.VectorSearchPort;
import com.ai.domain.model.Document;
import com.ai.domain.model.DocumentChunk;
import com.ai.domain.vo.DocumentId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Upload document use case.
 * Orchestrates document upload, chunking, embedding, and vector storage.
 */
public class UploadDocumentUseCase {
    private static final Logger log = LoggerFactory.getLogger(UploadDocumentUseCase.class);

    private final int chunkSize;
    private final int chunkOverlap;
    private final DocumentRepositoryPort documentRepository;
    private final EmbeddingPort embeddingPort;
    private final VectorSearchPort vectorSearchPort;

    public UploadDocumentUseCase(
            DocumentRepositoryPort documentRepository,
            EmbeddingPort embeddingPort,
            VectorSearchPort vectorSearchPort,
            int chunkSize,
            int chunkOverlap) {
        this.documentRepository = documentRepository;
        this.embeddingPort = embeddingPort;
        this.vectorSearchPort = vectorSearchPort;
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    @Transactional
    public Document execute(String title, String fileName, Long fileSize, String content) {
        log.info("Uploading document: {}", title);

        // 1. Create and persist document in UPLOADING state
        Document document = new Document(DocumentId.generate(), title, fileName, fileSize);
        document.markProcessing();
        document = documentRepository.save(document);
        documentRepository.flush();

        try {
            // 2. Chunk the content
            List<String> chunks = chunkText(content);
            log.info("Split into {} chunks", chunks.size());

            // 3. Embed each chunk and save to vector store
            List<DocumentChunk> embeddedChunks = new ArrayList<>();
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
                embeddedChunks.add(chunk);
            }

            // 4. Mark document as ready
            document.markReady();
            document = documentRepository.save(document);
            log.info("Document uploaded successfully: {}", document.getId());
            return document;

        } catch (Exception e) {
            log.error("Failed to process document", e);
            document.markFailed();
            documentRepository.save(document);
            throw new RuntimeException("Failed to process document: " + e.getMessage(), e);
        }
    }

    private List<String> chunkText(String text) {
        // Text chunking logic...
        return chunks;
    }
}
```

The use case orchestrates the workflow but delegates business logic to domain objects. The `Document` entity encapsulates status transition rules; the use case merely calls `markProcessing()`, `markReady()`, or `markFailed()`.

#### TypeScript Variant (Port Interface)

```typescript
// Application layer defines port interface
// Infrastructure layer implements it

export interface DocumentRepositoryPort {
  save(document: Document): Promise<Document>;
  findById(id: DocumentId): Promise<Document | null>;
  findAll(): Promise<Document[]>;
  delete(id: DocumentId): Promise<void>;
}

export interface EmbeddingPort {
  embed(text: string): Promise<number[]>;
}

export interface VectorSearchPort {
  saveChunk(chunk: DocumentChunk): Promise<void>;
  search(queryEmbedding: number[], topK: number): Promise<SearchResult[]>;
}
```

### Application Service (Orchestration)

Application services coordinate multiple use cases and manage the flow of data between the interface layer and the domain. They are the entry points for the interface layer when complex orchestration is needed.

#### Good (Java)

```java
package com.ai.application.service;

import com.ai.application.port.DocumentRepositoryPort;
import com.ai.application.usecase.*;
import com.ai.domain.model.Document;
import com.ai.application.usecase.RagChatUseCase.RetrievalResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * RAG application service.
 * Orchestrates document management and RAG chat operations.
 */
public class RagApplicationService {
    private static final Logger log = LoggerFactory.getLogger(RagApplicationService.class);

    private final UploadDocumentUseCase uploadDocumentUseCase;
    private final DeleteDocumentUseCase deleteDocumentUseCase;
    private final RagChatUseCase ragChatUseCase;
    private final DocumentRepositoryPort documentRepository;

    public RagApplicationService(
            UploadDocumentUseCase uploadDocumentUseCase,
            DeleteDocumentUseCase deleteDocumentUseCase,
            RagChatUseCase ragChatUseCase,
            DocumentRepositoryPort documentRepository) {
        this.uploadDocumentUseCase = uploadDocumentUseCase;
        this.deleteDocumentUseCase = deleteDocumentUseCase;
        this.ragChatUseCase = ragChatUseCase;
        this.documentRepository = documentRepository;
    }

    public Document uploadDocument(String title, String fileName, Long fileSize, String content) {
        Document document = uploadDocumentUseCase.execute(title, fileName, fileSize, content);
        log.info("Upload completed. Document id={}, title={}, status={}",
                document.getId(), document.getTitle(), document.getStatus());
        return document;
    }

    public List<Document> listDocuments() {
        return documentRepository.findAll();
    }

    public void deleteDocument(UUID documentId) {
        deleteDocumentUseCase.execute(documentId);
    }

    public RetrievalResult retrieveContext(String query, List<UUID> docIds, int topK) {
        return ragChatUseCase.execute(query, docIds, topK);
    }
}
```

The application service delegates to use cases rather than implementing logic directly. This composition makes the service easy to test and allows use cases to be reused in different contexts.

### Port Interface (Dependency Inversion)

Ports define contracts that the application layer requires from external systems. The Dependency Inversion Principle states that high-level modules should not depend on low-level modules; both should depend on abstractions.

#### Good (Java)

```java
package com.ai.application.port;

import com.ai.domain.model.ChatMessage;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * AI chat port interface.
 * Application layer defines the contract; Infrastructure implements it.
 */
public interface AiChatPort {

    /**
     * Sends a message to AI and returns the response.
     */
    String chat(String userMessage);

    /**
     * Sends message history to AI and returns the response.
     */
    String chatWithHistory(List<ChatMessage> messages);

    /**
     * Sends a message to AI and returns a streaming response.
     */
    Flux<String> chatStream(String userMessage);
}
```

This interface is defined in the Application layer but implemented by `SpringAiChatAdapter` in the Infrastructure layer. The application layer is decoupled from the specific AI framework used.

### DTO / Command / Query Records

Data Transfer Objects carry data between layers. Commands represent intent to perform an action; Query objects represent a request for data. Records in Java 17+ provide a clean syntax for defining DTOs.

#### Good (Java)

```java
package com.ai.application.dto;

// Command for uploading a document
public record UploadDocumentCommand(
        String title,
        String fileName,
        Long fileSize,
        String content
) {
    public UploadDocumentCommand {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Content is required");
        }
    }
}

// Result from document upload
public record UploadDocumentResult(
        UUID documentId,
        String title,
        String status,
        Instant createdAt
) {
    public static UploadDocumentResult from(Document document) {
        return new UploadDocumentResult(
                document.getId().value(),
                document.getTitle(),
                document.getStatus().name(),
                document.getCreatedAt()
        );
    }
}
```

Commands should validate their inputs in the constructor and throw exceptions for invalid states. The `from(Document)` factory method provides a convenient way to transform domain objects into results.

## Anti-Patterns Caught by This Layer

### Business Rules in Application Service

#### Bad

```java
public class BadRagApplicationService {

    public Document uploadDocument(String title, String fileName, Long fileSize, String content) {
        // Business logic should not be here
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }

        if (fileSize > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File too large");
        }

        // Creating document with business rules here violates separation
        Document document = new Document(...);

        // Direct status manipulation
        document.status = DocumentStatus.PROCESSING;  // Should be markProcessing()

        return documentRepository.save(document);
    }
}
```

#### Good

```java
public class RagApplicationService {
    // Service delegates to use case; use case uses domain objects
    public Document uploadDocument(...) {
        return uploadDocumentUseCase.execute(title, fileName, fileSize, content);
    }
}
```

### Cross-Layer Calls

#### Bad

```java
public class UploadDocumentUseCase {

    public Document execute(...) {
        // Application layer directly calling infrastructure
        var jpaRepo = new JpaDocumentRepository(...);  // FORBIDDEN

        // Or worse: importing infrastructure classes
        import com.ai.infrastructure.adapter.ai.SpringAiChatAdapter;

        SpringAiChatAdapter adapter = new SpringAiChatAdapter(...);
    }
}
```

#### Good

```java
public class UploadDocumentUseCase {

    private final DocumentRepositoryPort documentRepository;
    private final EmbeddingPort embeddingPort;

    // Dependencies injected via constructor
    public UploadDocumentUseCase(
            DocumentRepositoryPort documentRepository,
            EmbeddingPort embeddingPort,
            ...) {
        this.documentRepository = documentRepository;
        this.embeddingPort = embeddingPort;
    }
}
```

## Real Reference in This Workspace

All application layer code in this workspace is located at:

```
apps/server/src/main/java/com/ai/application/
```

Key reference files demonstrating the patterns:

- `usecase/UploadDocumentUseCase.java` — use case with chunking, embedding, and vector storage orchestration
- `port/AiChatPort.java` — port interface for AI chat functionality
- `service/RagApplicationService.java` — application service coordinating multiple use cases

## Verification (ArchUnit)

### ArchUnit Rule Example

```java
package com.ai.application;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ApplicationLayerArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter()
            .importPackages("com.ai.application..");

    @Test
    void applicationLayerMustNotDependOnInfrastructure() {
        ArchRule rule = noClasses()
                .that().resideInPackage("com.ai.application..")
                .should().dependOnClassesThat()
                .resideInPackage("com.ai.infrastructure..");

        rule.check(classes);
    }

    @Test
    void applicationLayerMustNotDependOnInterfaces() {
        ArchRule rule = noClasses()
                .that().resideInPackage("com.ai.application..")
                .should().dependOnClassesThat()
                .resideInPackage("com.ai.interfaces..");

        rule.check(classes);
    }

    @Test
    void applicationLayerMayDependOnDomain() {
        // This test verifies the dependency direction
        ArchRule rule = noClasses()
                .that().resideInPackage("com.ai.application..")
                .should().dependOnClassesThat()
                .resideInPackage("com.ai.domain..");

        rule.check(classes);
    }
}
```

## Checklist

When working in or auditing the Application layer, verify:

- [ ] No imports from `com.ai.infrastructure.*` or `com.ai.interfaces.*`
- [ ] Use cases contain orchestration logic only, no business rules
- [ ] Port interfaces are defined in Application layer, not imported from Infrastructure
- [ ] Application services delegate to use cases, not domain services
- [ ] DTOs/Commands/Records used for data transformation
- [ ] Transaction boundaries are defined at use case level
- [ ] Domain objects are never instantiated with business logic in application layer
- [ ] ArchUnit tests pass for application layer independence

---

## Cross-References

- [`../code-quality.md`](../code-quality.md) — Quality gates and architecture verification
- [`../clean-code-naming.md`](../clean-code-naming.md) — Naming conventions for use cases
- [`../clean-code-functions.md`](../clean-code-functions.md) — Function design in use cases
- [`../clean-code-error-handling.md`](../clean-code-error-handling.md) — Error handling patterns
- [`../clean-code-testing.md`](../clean-code-testing.md) — Use case testing principles
- [`../../software-architecture/SKILL.md`](../../software-architecture/SKILL.md) — Software architecture methodology

## Based on `apps/server` current structure (commit `e251a5b2`)