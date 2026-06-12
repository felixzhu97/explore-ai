package com.ai.infrastructure.adapter.persistence;

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
}
