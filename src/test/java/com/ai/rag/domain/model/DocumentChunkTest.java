package com.ai.rag.domain.model;

import com.ai.rag.domain.model.DocumentChunk;
import com.ai.rag.domain.vo.DocumentId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DocumentChunk Entity Tests
 * 
 * Tests for DocumentChunk immutable value object following TDD principles:
 * - Naming convention: should_expected_result_when_condition
 * - Uses AAA pattern (Arrange-Act-Assert)
 * - Tests chunk creation, embedding, and immutability
 */
@DisplayName("DocumentChunk")
class DocumentChunkTest {

    private static final DocumentId TEST_ID = DocumentId.of(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
    private static final DocumentId TEST_DOCUMENT_ID = DocumentId.of(UUID.fromString("223e4567-e89b-12d3-a456-426614174001"));
    private static final String TEST_CONTENT = "This is a test chunk content.";
    private static final int TEST_CHUNK_INDEX = 0;

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("should create chunk with all fields")
        void shouldCreateChunkWithAllFields() {
            // Arrange
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("author", "test-author");

            // Act
            DocumentChunk chunk = DocumentChunk.create(
                TEST_ID, TEST_DOCUMENT_ID, TEST_CONTENT, TEST_CHUNK_INDEX, metadata
            );

            // Assert
            assertThat(chunk.getId()).isEqualTo(TEST_ID);
            assertThat(chunk.getDocumentId()).isEqualTo(TEST_DOCUMENT_ID);
            assertThat(chunk.getContent()).isEqualTo(TEST_CONTENT);
            assertThat(chunk.getChunkIndex()).isEqualTo(TEST_CHUNK_INDEX);
            assertThat(chunk.getMetadata()).isEqualTo(metadata);
            assertThat(chunk.getEmbedding()).isNull();
        }

        @Test
        @DisplayName("should handle null embedding initially")
        void shouldHandleNullEmbeddingInitially() {
            // Act
            DocumentChunk chunk = DocumentChunk.create(
                TEST_ID, TEST_DOCUMENT_ID, TEST_CONTENT, TEST_CHUNK_INDEX, new HashMap<>()
            );

            // Assert
            assertThat(chunk.getEmbedding()).isNull();
        }

        @Test
        @DisplayName("should create chunk with empty metadata")
        void shouldCreateChunkWithEmptyMetadata() {
            // Act
            DocumentChunk chunk = DocumentChunk.create(
                TEST_ID, TEST_DOCUMENT_ID, TEST_CONTENT, TEST_CHUNK_INDEX, Map.of()
            );

            // Assert
            assertThat(chunk.getMetadata()).isEmpty();
        }

        @Test
        @DisplayName("should initialize with current timestamp")
        void shouldInitializeWithCurrentTimestamp() {
            // Arrange
            Instant before = Instant.now();

            // Act
            DocumentChunk chunk = DocumentChunk.create(
                TEST_ID, TEST_DOCUMENT_ID, TEST_CONTENT, TEST_CHUNK_INDEX, Map.of()
            );

            // Assert
            Instant after = Instant.now();
            assertThat(chunk.getCreatedAt()).isAfterOrEqualTo(before);
            assertThat(chunk.getCreatedAt()).isBeforeOrEqualTo(after);
        }

        @Test
        @DisplayName("should correctly store and retrieve chunkIndex")
        void shouldCorrectlyStoreAndRetrieveChunkIndex() {
            // Act
            DocumentChunk chunk = DocumentChunk.create(
                TEST_ID, TEST_DOCUMENT_ID, TEST_CONTENT, 5, Map.of()
            );

            // Assert
            assertThat(chunk.getChunkIndex()).isEqualTo(5);
        }

        @Test
        @DisplayName("should handle empty content")
        void shouldHandleEmptyContent() {
            // Act
            DocumentChunk chunk = DocumentChunk.create(
                TEST_ID, TEST_DOCUMENT_ID, "", TEST_CHUNK_INDEX, Map.of()
            );

            // Assert
            assertThat(chunk.getContent()).isEmpty();
        }

        @Test
        @DisplayName("should handle special characters in content")
        void shouldHandleSpecialCharactersInContent() {
            // Arrange
            String specialContent = "Hello! ¿Cómo estás? 你好世界! 🎉 \"quotes\" and 'more'";

            // Act
            DocumentChunk chunk = DocumentChunk.create(
                TEST_ID, TEST_DOCUMENT_ID, specialContent, TEST_CHUNK_INDEX, Map.of()
            );

            // Assert
            assertThat(chunk.getContent()).isEqualTo(specialContent);
        }

        @Test
        @DisplayName("should throw exception for null id")
        void shouldThrowExceptionForNullId() {
            assertThatThrownBy(() -> DocumentChunk.create(
                null, TEST_DOCUMENT_ID, TEST_CONTENT, TEST_CHUNK_INDEX, Map.of()
            )).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should throw exception for null documentId")
        void shouldThrowExceptionForNullDocumentId() {
            assertThatThrownBy(() -> DocumentChunk.create(
                TEST_ID, null, TEST_CONTENT, TEST_CHUNK_INDEX, Map.of()
            )).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should throw exception for null content")
        void shouldThrowExceptionForNullContent() {
            assertThatThrownBy(() -> DocumentChunk.create(
                TEST_ID, TEST_DOCUMENT_ID, null, TEST_CHUNK_INDEX, Map.of()
            )).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("withEmbedding")
    class WithEmbedding {

        @Test
        @DisplayName("should return new instance with embedding set")
        void shouldReturnNewInstanceWithEmbeddingSet() {
            // Arrange
            DocumentChunk original = DocumentChunk.create(
                TEST_ID, TEST_DOCUMENT_ID, TEST_CONTENT, TEST_CHUNK_INDEX, Map.of()
            );
            float[] embedding = new float[]{0.1f, 0.2f, 0.3f};

            // Act
            DocumentChunk withEmbedding = original.withEmbedding(embedding);

            // Assert
            assertThat(withEmbedding.getEmbedding()).isEqualTo(embedding);
        }

        @Test
        @DisplayName("should not modify original chunk")
        void shouldNotModifyOriginalChunk() {
            // Arrange
            DocumentChunk original = DocumentChunk.create(
                TEST_ID, TEST_DOCUMENT_ID, TEST_CONTENT, TEST_CHUNK_INDEX, Map.of()
            );
            float[] embedding = new float[]{0.1f, 0.2f, 0.3f};

            // Act
            original.withEmbedding(embedding);

            // Assert
            assertThat(original.getEmbedding()).isNull();
        }

        @Test
        @DisplayName("should preserve other fields when adding embedding")
        void shouldPreserveOtherFieldsWhenAddingEmbedding() {
            // Arrange
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("key", "value");
            DocumentChunk original = DocumentChunk.create(
                TEST_ID, TEST_DOCUMENT_ID, TEST_CONTENT, TEST_CHUNK_INDEX, metadata
            );
            float[] embedding = new float[]{0.1f, 0.2f, 0.3f};

            // Act
            DocumentChunk withEmbedding = original.withEmbedding(embedding);

            // Assert
            assertThat(withEmbedding.getId()).isEqualTo(original.getId());
            assertThat(withEmbedding.getDocumentId()).isEqualTo(original.getDocumentId());
            assertThat(withEmbedding.getContent()).isEqualTo(original.getContent());
            assertThat(withEmbedding.getChunkIndex()).isEqualTo(original.getChunkIndex());
            assertThat(withEmbedding.getMetadata()).isEqualTo(original.getMetadata());
            assertThat(withEmbedding.getCreatedAt()).isEqualTo(original.getCreatedAt());
        }

        @Test
        @DisplayName("should support embedding with different sizes")
        void shouldSupportEmbeddingWithDifferentSizes() {
            // Arrange
            DocumentChunk original = DocumentChunk.create(
                TEST_ID, TEST_DOCUMENT_ID, TEST_CONTENT, TEST_CHUNK_INDEX, Map.of()
            );

            // Act
            DocumentChunk withSmallEmbedding = original.withEmbedding(new float[]{0.1f, 0.2f});
            float[] largeEmbedding = new float[768];
            DocumentChunk withLargeEmbedding = original.withEmbedding(largeEmbedding);

            // Assert
            assertThat(withSmallEmbedding.getEmbedding()).hasSize(2);
            assertThat(withLargeEmbedding.getEmbedding()).hasSize(768);
        }

        @Test
        @DisplayName("should return instance with zero embedding")
        void shouldReturnInstanceWithZeroEmbedding() {
            // Arrange
            DocumentChunk original = DocumentChunk.create(
                TEST_ID, TEST_DOCUMENT_ID, TEST_CONTENT, TEST_CHUNK_INDEX, Map.of()
            );
            float[] zeroEmbedding = new float[]{0.0f, 0.0f, 0.0f};

            // Act
            DocumentChunk withEmbedding = original.withEmbedding(zeroEmbedding);

            // Assert
            assertThat(withEmbedding.getEmbedding()).isEqualTo(zeroEmbedding);
        }
    }

    @Nested
    @DisplayName("Immutability")
    class Immutability {

        @Test
        @DisplayName("should create independent copies of metadata")
        void shouldCreateIndependentCopiesOfMetadata() {
            // Arrange
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("key", "original");
            DocumentChunk chunk = DocumentChunk.create(
                TEST_ID, TEST_DOCUMENT_ID, TEST_CONTENT, TEST_CHUNK_INDEX, metadata
            );

            // Act - modify the original map
            metadata.put("newKey", "newValue");

            // Assert - chunk should have independent copy
            assertThat(chunk.getMetadata()).doesNotContainKey("newKey");
        }

        @Test
        @DisplayName("should have independent instances when using withEmbedding")
        void shouldHaveIndependentInstancesWhenUsingWithEmbedding() {
            // Arrange
            DocumentChunk original = DocumentChunk.create(
                TEST_ID, TEST_DOCUMENT_ID, TEST_CONTENT, TEST_CHUNK_INDEX, Map.of()
            );
            float[] embedding1 = new float[]{0.1f};
            float[] embedding2 = new float[]{0.2f};

            // Act
            DocumentChunk instance1 = original.withEmbedding(embedding1);
            DocumentChunk instance2 = original.withEmbedding(embedding2);

            // Assert
            assertThat(instance1.getEmbedding()).isEqualTo(embedding1);
            assertThat(instance2.getEmbedding()).isEqualTo(embedding2);
            assertThat(original.getEmbedding()).isNull();
        }
    }

    @Nested
    @DisplayName("createWithEmbedding")
    class CreateWithEmbedding {

        @Test
        @DisplayName("should create chunk with embedding directly")
        void shouldCreateChunkWithEmbeddingDirectly() {
            // Arrange
            float[] embedding = new float[]{0.1f, 0.2f, 0.3f};

            // Act
            DocumentChunk chunk = DocumentChunk.createWithEmbedding(
                TEST_ID, TEST_DOCUMENT_ID, TEST_CONTENT, TEST_CHUNK_INDEX, Map.of(), embedding
            );

            // Assert
            assertThat(chunk.getEmbedding()).isEqualTo(embedding);
            assertThat(chunk.getId()).isEqualTo(TEST_ID);
        }
    }
}
