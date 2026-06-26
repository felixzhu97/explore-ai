package com.ai.rag.infrastructure.storage;

import com.ai.rag.domain.model.DocumentChunk;
import com.ai.rag.infrastructure.storage.DocumentChunkEntity;
import com.ai.rag.infrastructure.storage.JpaIDocumentChunkRepository;
import com.ai.rag.infrastructure.storage.SpringDataChunkRepository;
import com.ai.rag.domain.vo.DocumentId;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * JpaIDocumentChunkRepository Unit Tests
 * 
 * Tests using Mockito to mock external dependencies (Spring Data repositories):
 * - Naming convention: should_expected_result_when_condition
 * - Uses AAA pattern (Arrange-Act-Assert)
 * - Tests save/find operations and entity mapping
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaIDocumentChunkRepository")
class JpaIDocumentChunkRepositoryTest {

    @Mock
    private SpringDataChunkRepository chunkRepository;

    private JpaIDocumentChunkRepository repository;

    private static final DocumentId TEST_DOCUMENT_ID = DocumentId.of(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
    private static final DocumentId TEST_CHUNK_ID = DocumentId.of(UUID.fromString("223e4567-e89b-12d3-a456-426614174001"));

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        repository = new JpaIDocumentChunkRepository(chunkRepository, objectMapper);
    }

    @Nested
    @DisplayName("saveChunk(DocumentChunk)")
    class SaveChunk {

        @Test
        @DisplayName("should save chunk entity")
        void shouldSaveChunkEntity() {
            // Arrange
            DocumentChunk chunk = createTestChunk();
            ArgumentCaptor<DocumentChunkEntity> entityCaptor = ArgumentCaptor.forClass(DocumentChunkEntity.class);

            // Act
            repository.saveChunk(chunk);

            // Assert
            verify(chunkRepository).save(entityCaptor.capture());
            DocumentChunkEntity saved = entityCaptor.getValue();
            assertThat(saved.getId()).isEqualTo(TEST_CHUNK_ID.value());
            assertThat(saved.getDocumentId()).isEqualTo(TEST_DOCUMENT_ID.value());
            assertThat(saved.getContent()).isEqualTo("Test chunk content");
        }

        @Test
        @DisplayName("should convert float embedding to Float array")
        void shouldConvertFloatEmbeddingToFloatArray() {
            // Arrange
            float[] embedding = {0.1f, 0.2f, 0.3f, 0.4f};
            DocumentChunk chunk = createTestChunk().withEmbedding(embedding);
            ArgumentCaptor<DocumentChunkEntity> entityCaptor = ArgumentCaptor.forClass(DocumentChunkEntity.class);

            // Act
            repository.saveChunk(chunk);

            // Assert
            verify(chunkRepository).save(entityCaptor.capture());
            DocumentChunkEntity saved = entityCaptor.getValue();
            assertThat(saved.getEmbedding()).isEqualTo(new Float[]{0.1f, 0.2f, 0.3f, 0.4f});
        }

        @Test
        @DisplayName("should serialize metadata to JSON")
        void shouldSerializeMetadataToJson() {
            // Arrange
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", "test");
            metadata.put("page", 1);
            DocumentChunk chunk = DocumentChunk.create(
                    TEST_CHUNK_ID,
                    TEST_DOCUMENT_ID,
                    "Test content",
                    0,
                    metadata
            );
            ArgumentCaptor<DocumentChunkEntity> entityCaptor = ArgumentCaptor.forClass(DocumentChunkEntity.class);

            // Act
            repository.saveChunk(chunk);

            // Assert
            verify(chunkRepository).save(entityCaptor.capture());
            DocumentChunkEntity saved = entityCaptor.getValue();
            assertThat(saved.getMetadata()).contains("\"source\":\"test\"");
            assertThat(saved.getMetadata()).contains("\"page\":1");
        }

        @Test
        @DisplayName("should handle null embedding")
        void shouldHandleNullEmbedding() {
            // Arrange
            DocumentChunk chunk = DocumentChunk.create(
                    TEST_CHUNK_ID,
                    TEST_DOCUMENT_ID,
                    "Test content",
                    0,
                    Map.of()
            );
            ArgumentCaptor<DocumentChunkEntity> entityCaptor = ArgumentCaptor.forClass(DocumentChunkEntity.class);

            // Act
            repository.saveChunk(chunk);

            // Assert
            verify(chunkRepository).save(entityCaptor.capture());
            assertThat(entityCaptor.getValue().getEmbedding()).isNull();
        }
    }

    @Nested
    @DisplayName("findChunksByDocumentId(UUID)")
    class FindChunksByDocumentId {

        @Test
        @DisplayName("should return chunks for document")
        void shouldReturnChunksForDocument() {
            // Arrange
            DocumentChunkEntity entity = createTestChunkEntity();
            when(chunkRepository.findByDocumentId(TEST_DOCUMENT_ID.value())).thenReturn(List.of(entity));

            // Act
            List<DocumentChunk> results = repository.findChunksByDocumentId(TEST_DOCUMENT_ID);

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getContent()).isEqualTo("Test chunk content");
        }

        @Test
        @DisplayName("should return empty list when no chunks")
        void shouldReturnEmptyListWhenNoChunks() {
            // Arrange
            when(chunkRepository.findByDocumentId(TEST_DOCUMENT_ID.value())).thenReturn(List.of());

            // Act
            List<DocumentChunk> results = repository.findChunksByDocumentId(TEST_DOCUMENT_ID);

            // Assert
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("deleteChunksByDocumentId(UUID)")
    class DeleteChunksByDocumentId {

        @Test
        @DisplayName("should delete chunks by document ID")
        void shouldDeleteChunksByDocumentId() {
            // Act
            repository.deleteChunksByDocumentId(TEST_DOCUMENT_ID);

            // Assert
            verify(chunkRepository).deleteByDocumentId(TEST_DOCUMENT_ID.value());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle null metadata when deserializing chunk")
        void shouldHandleNullMetadataWhenDeserializingChunk() {
            DocumentChunkEntity entity = new DocumentChunkEntity(
                    TEST_CHUNK_ID.value(),
                    TEST_DOCUMENT_ID.value(),
                    "Content without metadata",
                    0,
                    new Float[]{0.1f, 0.2f},
                    null,
                    Instant.now()
            );
            when(chunkRepository.findByDocumentId(TEST_DOCUMENT_ID.value())).thenReturn(List.of(entity));

            List<DocumentChunk> results = repository.findChunksByDocumentId(TEST_DOCUMENT_ID);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getMetadata()).isEmpty();
        }

        @Test
        @DisplayName("should handle empty metadata when deserializing chunk")
        void shouldHandleEmptyMetadataWhenDeserializingChunk() {
            DocumentChunkEntity entity = new DocumentChunkEntity(
                    TEST_CHUNK_ID.value(),
                    TEST_DOCUMENT_ID.value(),
                    "Content with empty metadata",
                    0,
                    null,
                    "",
                    Instant.now()
            );
            when(chunkRepository.findByDocumentId(TEST_DOCUMENT_ID.value())).thenReturn(List.of(entity));

            List<DocumentChunk> results = repository.findChunksByDocumentId(TEST_DOCUMENT_ID);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getMetadata()).isEmpty();
        }

        @Test
        @DisplayName("should handle null embedding when finding chunks")
        void shouldHandleNullEmbeddingWhenFindingChunks() {
            DocumentChunkEntity entity = new DocumentChunkEntity(
                    TEST_CHUNK_ID.value(),
                    TEST_DOCUMENT_ID.value(),
                    "Content",
                    0,
                    null,
                    "{}",
                    Instant.now()
            );
            when(chunkRepository.findByDocumentId(TEST_DOCUMENT_ID.value())).thenReturn(List.of(entity));

            List<DocumentChunk> results = repository.findChunksByDocumentId(TEST_DOCUMENT_ID);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getEmbedding()).isNull();
        }

        @Test
        @DisplayName("should handle invalid metadata JSON gracefully")
        void shouldHandleInvalidMetadataJsonGracefully() {
            DocumentChunkEntity entity = new DocumentChunkEntity(
                    TEST_CHUNK_ID.value(),
                    TEST_DOCUMENT_ID.value(),
                    "Content",
                    0,
                    null,
                    "invalid json {",
                    Instant.now()
            );
            when(chunkRepository.findByDocumentId(TEST_DOCUMENT_ID.value())).thenReturn(List.of(entity));

            List<DocumentChunk> results = repository.findChunksByDocumentId(TEST_DOCUMENT_ID);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getMetadata()).isEmpty();
        }

        @Test
        @DisplayName("should handle empty metadata map when saving chunk")
        void shouldHandleEmptyMetadataMapWhenSavingChunk() {
            DocumentChunk chunk = DocumentChunk.create(
                    TEST_CHUNK_ID,
                    TEST_DOCUMENT_ID,
                    "Test content",
                    0,
                    Map.of()
            );
            ArgumentCaptor<DocumentChunkEntity> entityCaptor = ArgumentCaptor.forClass(DocumentChunkEntity.class);

            repository.saveChunk(chunk);

            verify(chunkRepository).save(entityCaptor.capture());
            assertThat(entityCaptor.getValue().getMetadata()).isNull();
        }
    }

    private DocumentChunk createTestChunk() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "test");
        
        return DocumentChunk.create(
                TEST_CHUNK_ID,
                TEST_DOCUMENT_ID,
                "Test chunk content",
                0,
                metadata
        ).withEmbedding(new float[]{0.1f, 0.2f, 0.3f});
    }

    private DocumentChunkEntity createTestChunkEntity() {
        return new DocumentChunkEntity(
                TEST_CHUNK_ID.value(),
                TEST_DOCUMENT_ID.value(),
                "Test chunk content",
                0,
                new Float[]{0.1f, 0.2f, 0.3f},
                "{\"source\":\"test\"}",
                Instant.now()
        );
    }
}
