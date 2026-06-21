package com.ai.adapter.out.persistence;

import com.ai.modules.rag.domain.model.Document;
import com.ai.modules.rag.infrastructure.storage.DocumentEntity;
import com.ai.modules.rag.domain.model.DocumentStatus;
import com.ai.modules.rag.domain.vo.DocumentId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DocumentEntity")
class DocumentEntityTest {

    private static final UUID TEST_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("should create entity via no-arg constructor")
        void shouldCreateEntityViaNoArgConstructor() {
            DocumentEntity entity = new DocumentEntity();
            assertThat(entity).isNotNull();
        }

        @Test
        @DisplayName("should create entity with all fields via all-args constructor")
        void shouldCreateEntityWithAllArgsConstructor() {
            Instant now = Instant.now();

            DocumentEntity entity = new DocumentEntity(
                    TEST_ID, "title", "file.pdf", 1024L,
                    DocumentStatus.READY, now, now
            );

            assertThat(entity.getId()).isEqualTo(TEST_ID);
            assertThat(entity.getTitle()).isEqualTo("title");
            assertThat(entity.getFileName()).isEqualTo("file.pdf");
            assertThat(entity.getFileSize()).isEqualTo(1024L);
            assertThat(entity.getStatus()).isEqualTo(DocumentStatus.READY);
            assertThat(entity.getCreatedAt()).isEqualTo(now);
            assertThat(entity.getUpdatedAt()).isEqualTo(now);
        }
    }

    @Nested
    @DisplayName("Getters and Setters")
    class GettersAndSetters {

        @Test
        @DisplayName("should get and set id")
        void shouldGetAndSetId() {
            DocumentEntity entity = new DocumentEntity();
            entity.setId(TEST_ID);
            assertThat(entity.getId()).isEqualTo(TEST_ID);
        }

        @Test
        @DisplayName("should get and set title")
        void shouldGetAndSetTitle() {
            DocumentEntity entity = new DocumentEntity();
            entity.setTitle("new title");
            assertThat(entity.getTitle()).isEqualTo("new title");
        }

        @Test
        @DisplayName("should get and set fileName")
        void shouldGetAndSetFileName() {
            DocumentEntity entity = new DocumentEntity();
            entity.setFileName("report.pdf");
            assertThat(entity.getFileName()).isEqualTo("report.pdf");
        }

        @Test
        @DisplayName("should get and set fileSize")
        void shouldGetAndSetFileSize() {
            DocumentEntity entity = new DocumentEntity();
            entity.setFileSize(2048L);
            assertThat(entity.getFileSize()).isEqualTo(2048L);
        }

        @Test
        @DisplayName("should get and set status")
        void shouldGetAndSetStatus() {
            DocumentEntity entity = new DocumentEntity();
            entity.setStatus(DocumentStatus.PROCESSING);
            assertThat(entity.getStatus()).isEqualTo(DocumentStatus.PROCESSING);
        }

        @Test
        @DisplayName("should get and set createdAt")
        void shouldGetAndSetCreatedAt() {
            DocumentEntity entity = new DocumentEntity();
            Instant now = Instant.now();
            entity.setCreatedAt(now);
            assertThat(entity.getCreatedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("should get and set updatedAt")
        void shouldGetAndSetUpdatedAt() {
            DocumentEntity entity = new DocumentEntity();
            Instant now = Instant.now();
            entity.setUpdatedAt(now);
            assertThat(entity.getUpdatedAt()).isEqualTo(now);
        }
    }

    @Nested
    @DisplayName("fromDomain")
    class FromDomain {

        @Test
        @DisplayName("should create entity from domain Document with UPLOADING status")
        void shouldCreateEntityFromDomainWithUploadingStatus() {
            Document document = new Document(
                    DocumentId.of(TEST_ID),
                    "Test Title",
                    "test.pdf",
                    1024L
            );

            DocumentEntity entity = DocumentEntity.fromDomain(document);

            assertThat(entity.getId()).isEqualTo(TEST_ID);
            assertThat(entity.getTitle()).isEqualTo("Test Title");
            assertThat(entity.getFileName()).isEqualTo("test.pdf");
            assertThat(entity.getFileSize()).isEqualTo(1024L);
            assertThat(entity.getStatus()).isEqualTo(DocumentStatus.UPLOADING);
        }

        @Test
        @DisplayName("should create entity from domain Document with PROCESSING status")
        void shouldCreateEntityFromDomainWithProcessingStatus() {
            Document document = new Document(
                    DocumentId.of(TEST_ID),
                    "Processing Doc",
                    "doc.pdf",
                    2048L,
                    DocumentStatus.PROCESSING,
                    Instant.now(),
                    Instant.now()
            );

            DocumentEntity entity = DocumentEntity.fromDomain(document);

            assertThat(entity.getStatus()).isEqualTo(DocumentStatus.PROCESSING);
        }

        @Test
        @DisplayName("should create entity from domain Document with READY status")
        void shouldCreateEntityFromDomainWithReadyStatus() {
            Document document = new Document(
                    DocumentId.of(TEST_ID),
                    "Ready Doc",
                    "ready.pdf",
                    4096L,
                    DocumentStatus.READY,
                    Instant.now(),
                    Instant.now()
            );

            DocumentEntity entity = DocumentEntity.fromDomain(document);

            assertThat(entity.getStatus()).isEqualTo(DocumentStatus.READY);
        }

        @Test
        @DisplayName("should create entity from domain Document with FAILED status")
        void shouldCreateEntityFromDomainWithFailedStatus() {
            Document document = new Document(
                    DocumentId.of(TEST_ID),
                    "Failed Doc",
                    "failed.pdf",
                    512L,
                    DocumentStatus.FAILED,
                    Instant.now(),
                    Instant.now()
            );

            DocumentEntity entity = DocumentEntity.fromDomain(document);

            assertThat(entity.getStatus()).isEqualTo(DocumentStatus.FAILED);
        }

        @Test
        @DisplayName("should handle null fileName")
        void shouldHandleNullFileName() {
            Document document = new Document(
                    DocumentId.of(TEST_ID),
                    "No File",
                    null,
                    null
            );

            DocumentEntity entity = DocumentEntity.fromDomain(document);

            assertThat(entity.getFileName()).isNull();
            assertThat(entity.getFileSize()).isNull();
        }
    }

    @Nested
    @DisplayName("toDomain")
    class ToDomain {

        @Test
        @DisplayName("should convert entity to domain Document")
        void shouldConvertEntityToDomainDocument() {
            Instant now = Instant.now();
            DocumentEntity entity = new DocumentEntity(
                    TEST_ID, "Domain Doc", "domain.pdf", 8192L,
                    DocumentStatus.READY, now, now
            );

            Document document = entity.toDomain();

            assertThat(document.getId().value()).isEqualTo(TEST_ID);
            assertThat(document.getTitle()).isEqualTo("Domain Doc");
            assertThat(document.getFileName()).isEqualTo("domain.pdf");
            assertThat(document.getFileSize()).isEqualTo(8192L);
        }

        @Test
        @DisplayName("should handle entity with null fileName")
        void shouldHandleEntityWithNullFileName() {
            DocumentEntity entity = new DocumentEntity();
            entity.setId(TEST_ID);
            entity.setTitle("No File Entity");
            entity.setFileName(null);
            entity.setFileSize(null);
            entity.setStatus(DocumentStatus.UPLOADING);
            entity.setCreatedAt(Instant.now());
            entity.setUpdatedAt(Instant.now());

            Document document = entity.toDomain();

            assertThat(document.getFileName()).isNull();
            assertThat(document.getFileSize()).isNull();
        }
    }

    @Nested
    @DisplayName("DocumentStatus Enum")
    class DocumentStatusEnum {

        @Test
        @DisplayName("should have all expected status values")
        void shouldHaveAllExpectedStatusValues() {
            DocumentStatus[] statuses = DocumentStatus.values();
            
            assertThat(statuses).containsExactlyInAnyOrder(
                DocumentStatus.UPLOADING,
                DocumentStatus.PROCESSING,
                DocumentStatus.READY,
                DocumentStatus.FAILED
            );
        }

        @Test
        @DisplayName("should find status by name")
        void shouldFindStatusByName() {
            assertThat(DocumentStatus.valueOf("UPLOADING"))
                    .isEqualTo(DocumentStatus.UPLOADING);
            assertThat(DocumentStatus.valueOf("PROCESSING"))
                    .isEqualTo(DocumentStatus.PROCESSING);
            assertThat(DocumentStatus.valueOf("READY"))
                    .isEqualTo(DocumentStatus.READY);
            assertThat(DocumentStatus.valueOf("FAILED"))
                    .isEqualTo(DocumentStatus.FAILED);
        }
    }
}
