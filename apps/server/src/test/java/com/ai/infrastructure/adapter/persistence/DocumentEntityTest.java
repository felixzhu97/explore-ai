package com.ai.adapter.out.persistence;

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
                    DocumentEntity.DocumentStatus.READY, now, now
            );

            assertThat(entity.getId()).isEqualTo(TEST_ID);
            assertThat(entity.getTitle()).isEqualTo("title");
            assertThat(entity.getFileName()).isEqualTo("file.pdf");
            assertThat(entity.getFileSize()).isEqualTo(1024L);
            assertThat(entity.getStatus()).isEqualTo(DocumentEntity.DocumentStatus.READY);
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
            entity.setStatus(DocumentEntity.DocumentStatus.PROCESSING);
            assertThat(entity.getStatus()).isEqualTo(DocumentEntity.DocumentStatus.PROCESSING);
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
            com.ai.domain.model.Document document = new com.ai.domain.model.Document(
                    com.ai.domain.vo.DocumentId.of(TEST_ID),
                    "Test Title",
                    "test.pdf",
                    1024L
            );

            DocumentEntity entity = DocumentEntity.fromDomain(document);

            assertThat(entity.getId()).isEqualTo(TEST_ID);
            assertThat(entity.getTitle()).isEqualTo("Test Title");
            assertThat(entity.getFileName()).isEqualTo("test.pdf");
            assertThat(entity.getFileSize()).isEqualTo(1024L);
            assertThat(entity.getStatus()).isEqualTo(DocumentEntity.DocumentStatus.UPLOADING);
        }

        @Test
        @DisplayName("should create entity from domain Document with PROCESSING status")
        void shouldCreateEntityFromDomainWithProcessingStatus() {
            com.ai.domain.model.Document document = new com.ai.domain.model.Document(
                    com.ai.domain.vo.DocumentId.of(TEST_ID),
                    "Processing Doc",
                    "doc.pdf",
                    2048L,
                    com.ai.domain.model.DocumentStatus.PROCESSING,
                    Instant.now(),
                    Instant.now()
            );

            DocumentEntity entity = DocumentEntity.fromDomain(document);

            assertThat(entity.getStatus()).isEqualTo(DocumentEntity.DocumentStatus.PROCESSING);
        }

        @Test
        @DisplayName("should create entity from domain Document with READY status")
        void shouldCreateEntityFromDomainWithReadyStatus() {
            com.ai.domain.model.Document document = new com.ai.domain.model.Document(
                    com.ai.domain.vo.DocumentId.of(TEST_ID),
                    "Ready Doc",
                    "ready.pdf",
                    4096L,
                    com.ai.domain.model.DocumentStatus.READY,
                    Instant.now(),
                    Instant.now()
            );

            DocumentEntity entity = DocumentEntity.fromDomain(document);

            assertThat(entity.getStatus()).isEqualTo(DocumentEntity.DocumentStatus.READY);
        }

        @Test
        @DisplayName("should create entity from domain Document with FAILED status")
        void shouldCreateEntityFromDomainWithFailedStatus() {
            com.ai.domain.model.Document document = new com.ai.domain.model.Document(
                    com.ai.domain.vo.DocumentId.of(TEST_ID),
                    "Failed Doc",
                    "failed.pdf",
                    512L,
                    com.ai.domain.model.DocumentStatus.FAILED,
                    Instant.now(),
                    Instant.now()
            );

            DocumentEntity entity = DocumentEntity.fromDomain(document);

            assertThat(entity.getStatus()).isEqualTo(DocumentEntity.DocumentStatus.FAILED);
        }

        @Test
        @DisplayName("should handle null fileName")
        void shouldHandleNullFileName() {
            com.ai.domain.model.Document document = new com.ai.domain.model.Document(
                    com.ai.domain.vo.DocumentId.of(TEST_ID),
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
                    DocumentEntity.DocumentStatus.READY, now, now
            );

            com.ai.domain.model.Document document = entity.toDomain();

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
            entity.setStatus(DocumentEntity.DocumentStatus.UPLOADING);
            entity.setCreatedAt(Instant.now());
            entity.setUpdatedAt(Instant.now());

            com.ai.domain.model.Document document = entity.toDomain();

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
            DocumentEntity.DocumentStatus[] statuses = DocumentEntity.DocumentStatus.values();
            
            assertThat(statuses).containsExactlyInAnyOrder(
                DocumentEntity.DocumentStatus.UPLOADING,
                DocumentEntity.DocumentStatus.PROCESSING,
                DocumentEntity.DocumentStatus.READY,
                DocumentEntity.DocumentStatus.FAILED
            );
        }

        @Test
        @DisplayName("should find status by name")
        void shouldFindStatusByName() {
            assertThat(DocumentEntity.DocumentStatus.valueOf("UPLOADING"))
                    .isEqualTo(DocumentEntity.DocumentStatus.UPLOADING);
            assertThat(DocumentEntity.DocumentStatus.valueOf("PROCESSING"))
                    .isEqualTo(DocumentEntity.DocumentStatus.PROCESSING);
            assertThat(DocumentEntity.DocumentStatus.valueOf("READY"))
                    .isEqualTo(DocumentEntity.DocumentStatus.READY);
            assertThat(DocumentEntity.DocumentStatus.valueOf("FAILED"))
                    .isEqualTo(DocumentEntity.DocumentStatus.FAILED);
        }
    }
}
