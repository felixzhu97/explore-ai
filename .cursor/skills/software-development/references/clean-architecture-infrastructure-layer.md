# Clean Architecture — Infrastructure Layer

The Infrastructure layer is the outermost layer that implements the interfaces defined by the Domain and Application layers. It contains all the technical details: database persistence, external service clients, framework integrations, and configuration. This layer depends on the inner layers but inner layers know nothing about it.

## Layer Responsibilities

The Infrastructure layer is responsible for:

- **Implementing port interfaces** defined by the Application layer
- **Persisting domain entities** using database technologies (JPA, JDBC, MongoDB)
- **Integrating with external services** (AI providers, vector databases, messaging systems)
- **Handling cross-cutting concerns** (transactions, caching, security)
- **Providing configuration** for framework beans and external dependencies

Infrastructure code is inherently volatile — external libraries change, databases get replaced, and cloud providers evolve. By isolating this volatility in a single layer that depends inward, the Domain and Application layers remain stable and testable.

## Allowed / Forbidden Dependencies

### Allowed Dependencies

- Domain layer classes (`com.ai.domain.*`)
- Application layer classes (`com.ai.application.*`)
- All external frameworks and libraries (Spring, JPA, Hibernate, Spring AI, etc.)
- Database drivers, HTTP clients, message queue clients
- Configuration classes and properties

### Forbidden Dependencies

- Interface layer packages (`com.ai.interfaces.*`) — Infrastructure should not know about controllers or DTOs
- Business logic that belongs in Domain or Application layers

The Infrastructure layer can call into Application ports but should not call into Interface controllers. This ensures unidirectional data flow: Interface -> Application -> Domain <- Infrastructure.

## Skeleton (Directory Layout)

```
src/main/java/com/ai/infrastructure/
    adapter/                 # Adapters implementing application ports
        ai/
            SpringAiChatAdapter.java
            SpringAiChatService.java
            DocumentSearchTool.java
        embedding/
            OllamaEmbeddingAdapter.java
            MockEmbeddingAdapter.java
        document/
            PdfTextExtractor.java
        persistence/
            JpaDocumentRepository.java
            JpaChatSessionRepository.java
            DocumentEntity.java
            DocumentChunkEntity.java
            SpringDataDocumentRepository.java
            SpringDataChunkRepository.java
        vector/
            PgVectorAdapter.java
    config/                  # Spring configuration classes
        OllamaConfig.java
        PostgresConfig.java
        ApplicationConfig.java
        RagProperties.java
        WebCorsConfig.java
```

## Code Patterns

### JPA Entity

JPA entities live in the Infrastructure layer, separate from domain entities. They are data structures optimized for database mapping, not business logic carriers.

#### Good (Java)

```java
package com.ai.infrastructure.adapter.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for Document aggregate.
 * Lives in Infrastructure layer — separate from domain Document entity.
 */
@Entity
@Table(name = "documents")
public class DocumentEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DocumentStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // JPA-protected constructor
    protected DocumentEntity() {}

    public DocumentEntity(UUID id, String title, String fileName, Long fileSize,
                          DocumentStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and setters (JPA requires them)
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    // ... other getters/setters

    public enum DocumentStatus {
        UPLOADING, PROCESSING, READY, FAILED
    }
}
```

Key separation principle: `DocumentEntity` lives in Infrastructure, while `Document` (the domain entity) lives in the Domain layer. The adapter class handles mapping between them.

### Repository Implementation

Repository implementations live in Infrastructure and implement the port interfaces defined by the Domain or Application layers.

#### Good (Java)

```java
package com.ai.infrastructure.adapter.persistence;

import com.ai.application.port.DocumentRepositoryPort;
import com.ai.domain.model.Document;
import com.ai.domain.model.DocumentChunk;
import com.ai.domain.model.DocumentStatus;
import com.ai.domain.vo.DocumentId;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * JPA implementation of DocumentRepositoryPort.
 * Implements application port interface, uses Spring Data for persistence.
 */
@Component
public class JpaDocumentRepository implements DocumentRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(JpaDocumentRepository.class);

    private final SpringDataDocumentRepository documentRepository;
    private final SpringDataChunkRepository chunkRepository;
    private final ObjectMapper objectMapper;

    public JpaDocumentRepository(
            SpringDataDocumentRepository documentRepository,
            SpringDataChunkRepository chunkRepository,
            ObjectMapper objectMapper) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public Document save(Document document) {
        log.debug("Saving document: id={}, title={}", document.getId(), document.getTitle());

        DocumentEntity entity = toEntity(document);
        DocumentEntity saved = documentRepository.save(entity);

        log.info("Document saved successfully: id={}", saved.getId());
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Document> findById(UUID id) {
        log.debug("Finding document by id: {}", id);
        return documentRepository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        log.debug("Deleting document: id={}", id);
        chunkRepository.deleteByDocumentId(id);
        documentRepository.deleteById(id);
        log.info("Document deleted successfully: id={}", id);
    }

    // Mapping: Domain -> Entity
    private DocumentEntity toEntity(Document document) {
        DocumentEntity.DocumentStatus status;
        switch (document.getStatus()) {
            case UPLOADING -> status = DocumentEntity.DocumentStatus.UPLOADING;
            case PROCESSING -> status = DocumentEntity.DocumentStatus.PROCESSING;
            case READY -> status = DocumentEntity.DocumentStatus.READY;
            case FAILED -> status = DocumentEntity.DocumentStatus.FAILED;
            default -> throw new IllegalArgumentException("Unknown status: " + document.getStatus());
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

    // Mapping: Entity -> Domain
    private Document toDomain(DocumentEntity entity) {
        DocumentStatus status;
        switch (entity.getStatus()) {
            case UPLOADING -> status = DocumentStatus.UPLOADING;
            case PROCESSING -> status = DocumentStatus.PROCESSING;
            case READY -> status = DocumentStatus.READY;
            case FAILED -> status = DocumentStatus.FAILED;
            default -> throw new IllegalArgumentException("Unknown status: " + entity.getStatus());
        }

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

The repository implementation handles all mapping between domain objects and JPA entities. It is decorated with `@Component` for Spring dependency injection and `@Transactional` for transaction management.

### External Service Adapter

Adapters wrap external service clients and translate between the application's domain model and the external service's protocol.

#### Good (Java)

```java
package com.ai.infrastructure.adapter.ai;

import com.ai.application.port.AiChatPort;
import com.ai.domain.model.ChatMessage;
import com.ai.domain.service.AiChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Spring AI adapter — implements AiChatPort.
 * Wraps the domain AiChatService with Spring AI integration.
 */
@Component
public class SpringAiChatAdapter implements AiChatPort {

    private static final Logger log = LoggerFactory.getLogger(SpringAiChatAdapter.class);

    private final AiChatService aiChatService;

    public SpringAiChatAdapter(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @Override
    public String chat(String userMessage) {
        log.info("Sending message to AI: {}", truncateForLog(userMessage));
        try {
            String response = aiChatService.chat(userMessage);
            log.info("Received AI response: {}", truncateForLog(response));
            return response;
        } catch (Exception e) {
            log.error("AI chat failed", e);
            throw new RuntimeException("AI service error: " + e.getMessage(), e);
        }
    }

    @Override
    public String chatWithHistory(List<ChatMessage> messages) {
        log.info("Sending {} messages to AI with history", messages.size());
        try {
            return aiChatService.chatWithHistory(messages);
        } catch (Exception e) {
            log.error("AI chat with history failed", e);
            throw new RuntimeException("AI service error: " + e.getMessage(), e);
        }
    }

    @Override
    public Flux<String> chatStream(String userMessage) {
        log.info("Sending streaming message to AI: {}", truncateForLog(userMessage));
        try {
            return aiChatService.chatStream(userMessage)
                    .doOnNext(chunk -> log.debug("Stream chunk received: {} chars", chunk.length()))
                    .doOnError(e -> log.error("AI stream failed", e));
        } catch (Exception e) {
            log.error("AI chat stream failed", e);
            throw new RuntimeException("AI service error: " + e.getMessage(), e);
        }
    }

    private String truncateForLog(String text) {
        if (text == null) return "null";
        if (text.length() <= 100) return text;
        return text.substring(0, 100) + "...";
    }
}
```

The adapter implements the Application port interface (`AiChatPort`) while delegating to the Domain service interface (`AiChatService`). This double indirection allows the application to remain independent of both the AI framework and the specific domain service implementation.

### Configuration Classes

Configuration classes define Spring beans and manage external service connections.

#### Good (Java)

```java
package com.ai.infrastructure.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Ollama embedding service.
 * Conditionally creates an Ollama embedding model based on application properties.
 */
@Configuration
public class OllamaConfig {

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    @Bean
    @ConditionalOnProperty(name = "rag.mock.embeddings", havingValue = "false", matchIfMissing = true)
    public EmbeddingModel embeddingModel() {
        OllamaApi api = OllamaApi.builder()
                .baseUrl(baseUrl)
                .build();

        return OllamaEmbeddingModel.builder()
                .ollamaApi(api)
                .build();
    }
}
```

Configuration classes use Spring's `@Configuration` annotation and can use conditional bean creation to support different deployment scenarios.

## Anti-Patterns Caught by This Layer

### Domain Depending on JPA

#### Bad

```java
// In Domain layer
package com.ai.domain.model;

import jakarta.persistence.*;

@Entity
public class Document {
    @Id
    private UUID id;
    // ...
}
```

#### Good

```java
// Domain layer — no JPA annotations
package com.ai.domain.model;

public class Document {
    private final DocumentId id;
    // No JPA annotations
}

// Infrastructure layer — JPA entity
package com.ai.infrastructure.adapter.persistence;

@Entity
public class DocumentEntity {
    // JPA annotations here
}
```

### Infrastructure Calling Interface Layer

#### Bad

```java
// Infrastructure calling Interface (bidirectional dependency — FORBIDDEN)
public class BadAdapter {

    public void handleUpload(UploadDocumentRequest request) {
        // Directly using Interface layer DTO
        var responseDto = new UploadDocumentResponse(...);

        // Infrastructure should not know about Interface!
    }
}
```

#### Good

```java
// Infrastructure implements Application port, returns Domain objects
public class JpaDocumentRepository implements DocumentRepositoryPort {

    @Override
    public Document save(Document document) {
        // Infrastructure converts between Domain and Entity
        // Returns Domain object to Application
        return toDomain(entity);
    }
}
```

### Adapter Leaking Domain Types

#### Bad

```java
// Adapter returning domain entity directly to external system
public class BadAiAdapter implements AiChatPort {

    public List<ChatMessage> getHistory() {
        // Returning domain type to external caller
        return chatRepository.findHistory();
    }
}
```

#### Good

```java
// Adapter works with domain types internally
public class SpringAiChatAdapter implements AiChatPort {

    private final AiChatService aiChatService;

    @Override
    public String chat(String userMessage) {
        // Adapts external format to domain, returns domain result
        return aiChatService.chat(userMessage);
    }
}
```

## Real Reference in This Workspace

All infrastructure layer code in this workspace is located at:

```
apps/server/src/main/java/com/ai/infrastructure/
```

Key reference files demonstrating the patterns:

- `adapter/persistence/DocumentEntity.java` — JPA entity separate from domain `Document`
- `adapter/persistence/JpaDocumentRepository.java` — repository implementation with entity mappers
- `adapter/ai/SpringAiChatAdapter.java` — AI service adapter implementing `AiChatPort`
- `config/OllamaConfig.java` — conditional configuration for embedding model

## Verification (ArchUnit)

### ArchUnit Rule Example

```java
package com.ai.infrastructure;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

class InfrastructureLayerArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter()
            .importPackages("com.ai.infrastructure..");

    @Test
    void infrastructureMustNotDependOnInterfaces() {
        ArchRule rule = noClasses()
                .that().resideInPackage("com.ai.infrastructure..")
                .should().dependOnClassesThat()
                .resideInPackage("com.ai.interfaces..");

        rule.check(classes);
    }

    @Test
    void adaptersMayImplementApplicationPorts() {
        // Verify adapters implement port interfaces
        ArchRule rule = classes()
                .that().haveNameMatching(".*Adapter")
                .should().implement(com.ai.application.port.AiChatPort.class)
                .orShould().implement(com.ai.application.port.EmbeddingPort.class)
                .orShould().implement(com.ai.application.port.VectorSearchPort.class);

        rule.check(classes);
    }

    @Test
    void jpaEntitiesShouldBeInPersistencePackage() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(jakarta.persistence.Entity.class)
                .should().resideInPackage("..adapter.persistence..");

        rule.check(classes);
    }
}
```

## TypeScript

TypeScript infrastructure code follows similar patterns but is less common in this stack. The primary consideration is ensuring that infrastructure implementations are imported via dependency injection and not referenced directly in domain or application code.

---

## Cross-References

- [`../code-quality.md`](../code-quality.md) — Quality gates and architecture verification
- [`../clean-code-naming.md`](../clean-code-naming.md) — Naming conventions for adapters
- [`../clean-code-functions.md`](../clean-code-functions.md) — Adapter function design
- [`../clean-code-error-handling.md`](../clean-code-error-handling.md) — Error handling in adapters
- [`../clean-code-testing.md`](../clean-code-testing.md) — Integration testing for adapters
- [`../../software-architecture/SKILL.md`](../../software-architecture/SKILL.md) — Software architecture methodology

## Based on `apps/server` current structure (commit `e251a5b2`)