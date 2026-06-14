# Clean Architecture — Domain Layer

The Domain layer is the innermost, most stable layer of Clean Architecture. It contains the core business logic, entities, value objects, and business rules that define the application without depending on any external frameworks, databases, or UI frameworks.

## Layer Responsibilities

The Domain layer is responsible for:

- **Expressing business concepts** through entities and value objects that model the problem domain
- **Encapsulating business rules** that govern state transitions and validations
- **Defining repository interfaces** that describe data access contracts
- **Publishing domain events** that capture significant business occurrences
- **Providing domain services** for operations that span multiple entities

The Domain layer must remain completely independent of infrastructure concerns. It contains no imports from Spring, Jakarta EE, JPA, Hibernate, or any other external framework. This independence ensures the business logic is testable in isolation and remains stable as the surrounding infrastructure evolves.

## Allowed / Forbidden Dependencies

### Allowed Dependencies

- Java standard library classes (`java.time`, `java.util`, `java.math`)
- Other domain layer classes within the same bounded context
- Other domain layer packages within the same module

### Forbidden Dependencies

The following are strictly prohibited in the Domain layer:

- `org.springframework.*` — no Spring annotations, components, or dependencies
- `jakarta.persistence.*` or `javax.persistence.*` — no JPA/Hibernate annotations
- `org.hibernate.*` — no Hibernate types or annotations
- `org.junit.*` or `org.mockito.*` — test dependencies belong in the test source set
- Any infrastructure layer package (`com.ai.infrastructure.*`, `com.ai.interfaces.*`)
- Any application layer package (`com.ai.application.*`) except for domain event definitions

Any violation of these rules indicates a Clean Architecture boundary breach that will erode the long-term maintainability of the codebase.

## Skeleton (Directory Layout)

```
src/main/java/com/ai/domain/
    model/                  # Aggregates and entities
        Document.java
        DocumentChunk.java
        ChatSession.java
        ChatMessage.java
        DocumentNotFoundException.java
        AiServiceException.java
    vo/                     # Value objects
        DocumentId.java
        ChatSessionId.java
        MessageId.java
        MessageContent.java
    repository/             # Repository interfaces (Domain defines, Infrastructure implements)
        DocumentRepositoryPort.java
        ChatSessionRepositoryPort.java
    service/                # Domain services (cross-entity operations)
        AiChatService.java   # Interface defined here, implemented in infrastructure
    event/                  # Domain events
        DomainEvent.java
        ChatMessageReceivedEvent.java
        ChatResponseGeneratedEvent.java
    exception/              # Domain-specific exceptions
        RagServiceException.java
        DocumentNotFoundException.java
```

Note: The workspace uses `DocumentRepositoryPort` and `ChatSessionRepositoryPort` interfaces defined under `com.ai.domain.repository`, following the convention where ports are co-located with their domain context.

## Code Patterns

### Aggregate Root

An aggregate root is the sole entry point for all operations on a group of related entities and value objects. External code must interact with the aggregate only through the root, ensuring internal consistency is always maintained.

#### Good (Java)

```java
package com.ai.domain.model;

/**
 * Document aggregate root.
 * Manages document lifecycle states and ensures consistency invariants.
 */
public class Document {
    private final DocumentId id;       // Immutable identifier
    private String title;
    private String fileName;
    private Long fileSize;
    private DocumentStatus status;     // Mutable state, controlled by business methods
    private final Instant createdAt;   // Immutable once set
    private Instant updatedAt;

    // Private constructor enforces use of factory methods
    private Document(DocumentId id, String title, String fileName, Long fileSize) {
        this.id = id;
        this.title = title;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.status = DocumentStatus.UPLOADING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Factory method for creating new documents
    public static Document create(String title, String fileName, Long fileSize) {
        return new Document(DocumentId.generate(), title, fileName, fileSize);
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

    // Business methods control all state transitions
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

    public boolean isReady() {
        return this.status == DocumentStatus.READY;
    }

    // Getters only — no public setters
    public DocumentId getId() { return id; }
    public String getTitle() { return title; }
    public DocumentStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    // ... other getters
}
```

This pattern ensures that every state transition goes through a business method that can enforce invariants. The repository uses the full constructor to reconstitute entities from the database without triggering business validations.

### Value Object (Java Record)

Value objects are immutable, have no identity, and are compared by their attribute values. Java records provide a concise syntax for defining value objects with built-in immutability and `equals`/`hashCode` implementations.

#### Good (Java Record)

```java
package com.ai.domain.vo;

/**
 * DocumentId Value Object.
 * Wraps UUID to provide type safety and domain semantics.
 */
public final class DocumentId {
    private final UUID value;

    private DocumentId(UUID value) {
        this.value = Objects.requireNonNull(value, "UUID cannot be null");
    }

    public static DocumentId of(UUID uuid) {
        return new DocumentId(uuid);
    }

    public static DocumentId of(String uuidString) {
        if (uuidString == null || uuidString.isBlank()) {
            throw new IllegalArgumentException("UUID string cannot be null or blank");
        }
        return new DocumentId(UUID.fromString(uuidString));
    }

    public static DocumentId generate() {
        return new DocumentId(UUID.randomUUID());
    }

    public UUID value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocumentId that = (DocumentId) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
```

The private constructor prevents direct instantiation, forcing callers through factory methods that can perform validation. The `equals` and `hashCode` implementations ensure value-based comparison semantics.

#### TypeScript Variant

```typescript
// TypeScript: Value objects as branded types with factory functions
type DocumentIdBrand = { readonly __brand: 'DocumentId' };
export type DocumentId = string & DocumentIdBrand;

export function DocumentId(value: string): DocumentId {
  if (!value || !isValidUUID(value)) {
    throw new Error('Invalid UUID format');
  }
  return value as DocumentId;
}

export function DocumentIdGenerate(): DocumentId {
  return crypto.randomUUID() as DocumentId;
}

export function DocumentIdFromString(value: string): DocumentId {
  return DocumentId(value);
}
```

The branded type pattern in TypeScript provides compile-time type safety while maintaining runtime compatibility with `string`. The factory function validates input at the boundary where the value object is created.

### Domain Event

Domain events capture significant occurrences in the business domain. They are immutable records that represent facts that have happened. Aggregates publish events when important state changes occur, and other parts of the system can subscribe to react to these events.

#### Good (Java)

```java
package com.ai.domain.event;

import java.time.Instant;

/**
 * Base interface for all domain events.
 * Domain events are immutable records of things that have happened.
 */
public abstract sealed class DomainEvent permits
        ChatMessageReceivedEvent,
        ChatResponseGeneratedEvent {

    private final Instant occurredAt;

    protected DomainEvent(Instant occurredAt) {
        this.occurredAt = Objects.requireNonNull(occurredAt);
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public abstract String getEventType();
}

/**
 * Event published when a user sends a chat message.
 */
public final class ChatMessageReceivedEvent extends DomainEvent {

    private final ChatSessionId sessionId;
    private final MessageId messageId;
    private final String content;

    public ChatMessageReceivedEvent(ChatSessionId sessionId, MessageId messageId,
                                    String content, Instant occurredAt) {
        super(occurredAt);
        this.sessionId = sessionId;
        this.messageId = messageId;
        this.content = content;
    }

    public ChatSessionId getSessionId() { return sessionId; }
    public MessageId getMessageId() { return messageId; }
    public String getContent() { return content; }

    @Override
    public String getEventType() {
        return "ChatMessageReceived";
    }
}
```

Using sealed classes for domain events provides exhaustive pattern matching capabilities and documents all possible event types in one place. Events are created by aggregates when significant business operations occur.

### Domain Service Interface

Domain services define contracts for operations that do not naturally belong to a single entity. These are typically cross-aggregate operations or operations that depend on external services.

#### Good (Java)

```java
package com.ai.domain.service;

import com.ai.domain.model.ChatMessage;
import java.util.List;

/**
 * AI chat service interface.
 * Defined in Domain layer, implemented in Infrastructure layer.
 * This is a domain service because AI interaction spans multiple aggregates.
 */
public interface AiChatService {

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
    reactor.core.publisher.Flux<String> chatStream(String userMessage);
}
```

The interface is defined in the domain layer, but the implementation resides in the infrastructure layer. This allows the domain to express its requirements without depending on any specific AI framework.

### Repository Interface

Repository interfaces are defined in the Domain layer, following the Dependency Inversion Principle. The Domain layer specifies what data operations it needs, and the Infrastructure layer provides implementations.

#### Good (Java)

```java
package com.ai.domain.repository;

import com.ai.domain.model.Document;
import com.ai.domain.model.DocumentChunk;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Document repository port.
 * Domain defines the interface; Infrastructure implements it.
 */
public interface DocumentRepositoryPort {

    Optional<Document> findById(UUID id);

    List<Document> findAll();

    Document save(Document document);

    void flush();

    void delete(UUID id);

    // Chunk operations for document processing
    void saveChunk(DocumentChunk chunk);

    List<DocumentChunk> findChunksByDocumentId(UUID documentId);

    void deleteChunksByDocumentId(UUID documentId);
}
```

The repository port operates on aggregate roots and value objects, never on JPA entities. The implementation in the infrastructure layer handles the mapping between domain objects and persistence mechanisms.

## Anti-Patterns Caught by This Layer

### Spring/JPA Annotation Leakage

#### Bad

```java
package com.ai.domain.model;

import org.springframework.stereotype.Component;  // FORBIDDEN
import jakarta.persistence.*;

@Entity
public class Document {
    @Id
    @GeneratedValue
    private UUID id;
    // ...
}
```

#### Good

```java
package com.ai.domain.model;

import com.ai.domain.vo.DocumentId;
// No framework imports

public class Document {
    private final DocumentId id;  // Plain Java field
    // ...
}
```

### Anemic Domain Model

#### Bad

```java
public class Document {
    private UUID id;
    private String title;
    private DocumentStatus status;

    // Only getters and setters — no behavior
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    // ... all fields have only getters/setters
}
```

#### Good

```java
public class Document {
    private final DocumentId id;
    private DocumentStatus status;

    // Business method encapsulates state transition
    public void markReady() {
        if (this.status != DocumentStatus.PROCESSING) {
            throw new IllegalStateException("Document must be processing before marking ready");
        }
        this.status = DocumentStatus.READY;
    }
}
```

### Exposing Mutable Collections

#### Bad

```java
public class Document {
    private List<DocumentChunk> chunks;

    public List<DocumentChunk> getChunks() {
        return chunks;  // Caller can modify internal state!
    }
}
```

#### Good

```java
public class Document {
    private List<DocumentChunk> chunks;

    public List<DocumentChunk> getChunks() {
        return Collections.unmodifiableList(chunks);
    }
}
```

## Real Reference in This Workspace

All domain layer code in this workspace is located at:

```
apps/server/src/main/java/com/ai/domain/
```

Key reference files demonstrating the patterns:

- `model/Document.java` — aggregate root with business methods for status transitions
- `vo/DocumentId.java` — value object with factory methods and validation
- `event/ChatMessageReceivedEvent.java` — domain event definition
- `service/AiChatService.java` — domain service interface
- `repository/DocumentRepositoryPort.java` — repository interface

## Verification (ArchUnit / dependency-cruiser)

### ArchUnit Rule Example

```java
package com.ai.domain;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.elements.GivenClasses;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class DomainLayerArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter()
            .importPackages("com.ai.domain..");

    @Test
    void domainLayerMustNotDependOnSpring() {
        ArchRule rule = noClasses()
                .that().resideInPackage("com.ai.domain..")
                .should().dependOnClassesThat()
                .resideInPackage("org.springframework..");

        rule.check(classes);
    }

    @Test
    void domainLayerMustNotDependOnJPA() {
        ArchRule rule = noClasses()
                .that().resideInPackage("com.ai.domain..")
                .should().dependOnClassesThat()
                .resideInPackage("jakarta.persistence..");

        rule.check(classes);
    }

    @Test
    void domainLayerMustNotDependOnInfrastructure() {
        ArchRule rule = noClasses()
                .that().resideInPackage("com.ai.domain..")
                .should().dependOnClassesThat()
                .resideInPackage("com.ai.infrastructure..");

        rule.check(classes);
    }

    @Test
    void domainLayerMustNotDependOnInterfaces() {
        ArchRule rule = noClasses()
                .that().resideInPackage("com.ai.domain..")
                .should().dependOnClassesThat()
                .resideInPackage("com.ai.interfaces..");

        rule.check(classes);
    }

    @Test
    void domainModelsMustNotHaveSpringAnnotations() {
        ArchRule rule = noClasses()
                .that().resideInPackage("com.ai.domain.model..")
                .should().beAnnotatedWith("org.springframework.stereotype.Component")
                .orShould().beAnnotatedWith("org.springframework.stereotype.Service")
                .orShould().beAnnotatedWith("org.springframework.stereotype.Repository");

        rule.check(classes);
    }
}
```

These ArchUnit tests run as part of the continuous integration pipeline and will fail if any developer accidentally introduces framework dependencies into the domain layer.

## Checklist

When working in or auditing the Domain layer, verify:

- [ ] No imports from `org.springframework.*`, `jakarta.persistence.*`, or `javax.persistence.*`
- [ ] No annotations like `@Entity`, `@Component`, `@Service` on domain classes
- [ ] Entities have business methods, not just getters and setters
- [ ] All state changes go through business methods that can enforce invariants
- [ ] No public setters on entities
- [ ] Collections returned from entities are immutable views
- [ ] Value objects are immutable (all fields final, no setters)
- [ ] Repository interfaces are defined in Domain, not imported from Infrastructure
- [ ] Domain events are immutable records
- [ ] Domain services are interfaces defined in Domain, implemented in Infrastructure
- [ ] ArchUnit tests pass for domain layer independence

---

## Cross-References

- [`../code-quality.md`](../code-quality.md) — Quality gates and architecture verification
- [`../clean-code-naming.md`](../clean-code-naming.md) — Naming conventions for domain objects
- [`../clean-code-functions.md`](../clean-code-functions.md) — Business method design
- [`../clean-code-comments.md`](../clean-code-comments.md) — Domain documentation standards
- [`../clean-code-error-handling.md`](../clean-code-error-handling.md) — Domain exception patterns
- [`../clean-code-formatting.md`](../clean-code-formatting.md) — Code formatting rules
- [`../clean-code-testing.md`](../clean-code-testing.md) — Domain unit testing principles
- [`../../software-architecture/SKILL.md`](../../software-architecture/SKILL.md) — Software architecture methodology

## Based on `apps/server` current structure (commit `e251a5b2`)