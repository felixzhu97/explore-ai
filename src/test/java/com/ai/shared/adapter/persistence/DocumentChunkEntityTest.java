package com.ai.adapter.out.persistence;

import com.ai.rag.infrastructure.storage.DocumentChunkEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DocumentChunkEntity")
class DocumentChunkEntityTest {

    private static final UUID TEST_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final UUID TEST_DOCUMENT_ID = UUID.fromString("223e4567-e89b-12d3-a456-426614174001");

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("should create entity via no-arg constructor")
        void shouldCreateEntityViaNoArgConstructor() {
            DocumentChunkEntity entity = new DocumentChunkEntity();
            assertThat(entity).isNotNull();
        }

        @Test
        @DisplayName("should create entity with all fields via all-args constructor")
        void shouldCreateEntityWithAllArgsConstructor() {
            Instant now = Instant.now();
            Float[] embedding = new Float[]{0.1f, 0.2f, 0.3f};

            DocumentChunkEntity entity = new DocumentChunkEntity(
                    TEST_ID, TEST_DOCUMENT_ID, "content", 0, embedding, "{}", now
            );

            assertThat(entity.getId()).isEqualTo(TEST_ID);
            assertThat(entity.getDocumentId()).isEqualTo(TEST_DOCUMENT_ID);
            assertThat(entity.getContent()).isEqualTo("content");
            assertThat(entity.getChunkIndex()).isZero();
            assertThat(entity.getEmbedding()).isEqualTo(embedding);
            assertThat(entity.getMetadata()).isEqualTo("{}");
            assertThat(entity.getCreatedAt()).isEqualTo(now);
        }
    }

    @Nested
    @DisplayName("Getters and Setters")
    class GettersAndSetters {

        @Test
        @DisplayName("should get and set id")
        void shouldGetAndSetId() {
            DocumentChunkEntity entity = new DocumentChunkEntity();
            entity.setId(TEST_ID);
            assertThat(entity.getId()).isEqualTo(TEST_ID);
        }

        @Test
        @DisplayName("should get and set documentId")
        void shouldGetAndSetDocumentId() {
            DocumentChunkEntity entity = new DocumentChunkEntity();
            entity.setDocumentId(TEST_DOCUMENT_ID);
            assertThat(entity.getDocumentId()).isEqualTo(TEST_DOCUMENT_ID);
        }

        @Test
        @DisplayName("should get and set content")
        void shouldGetAndSetContent() {
            DocumentChunkEntity entity = new DocumentChunkEntity();
            entity.setContent("hello world");
            assertThat(entity.getContent()).isEqualTo("hello world");
        }

        @Test
        @DisplayName("should get and set chunkIndex")
        void shouldGetAndSetChunkIndex() {
            DocumentChunkEntity entity = new DocumentChunkEntity();
            entity.setChunkIndex(5);
            assertThat(entity.getChunkIndex()).isEqualTo(5);
        }

        @Test
        @DisplayName("should get and set embedding")
        void shouldGetAndSetEmbedding() {
            DocumentChunkEntity entity = new DocumentChunkEntity();
            Float[] embedding = new Float[]{0.5f, 0.6f};
            entity.setEmbedding(embedding);
            assertThat(entity.getEmbedding()).isEqualTo(embedding);
        }

        @Test
        @DisplayName("should get and set metadata")
        void shouldGetAndSetMetadata() {
            DocumentChunkEntity entity = new DocumentChunkEntity();
            entity.setMetadata("{\"page\":1}");
            assertThat(entity.getMetadata()).isEqualTo("{\"page\":1}");
        }

        @Test
        @DisplayName("should get and set createdAt")
        void shouldGetAndSetCreatedAt() {
            DocumentChunkEntity entity = new DocumentChunkEntity();
            Instant now = Instant.now();
            entity.setCreatedAt(now);
            assertThat(entity.getCreatedAt()).isEqualTo(now);
        }
    }
}
