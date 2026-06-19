package com.ai.adapter.out.persistence;

import com.ai.adapter.out.persistence.JpaDocumentRepository;
import com.ai.domain.model.Document;
import com.ai.domain.model.DocumentChunk;
import com.ai.domain.model.DocumentStatus;
import com.ai.domain.vo.DocumentId;
import com.ai.adapter.out.persistence.*;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * JpaDocumentRepository Unit Tests
 * 
 * Tests using Mockito to mock external dependencies (Spring Data repositories):
 * - Naming convention: should_expected_result_when_condition
 * - Uses AAA pattern (Arrange-Act-Assert)
 * - Tests save/find operations and entity mapping
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaDocumentRepository")
class JpaDocumentRepositoryTest {

    @Mock
    private SpringDataDocumentRepository documentRepository;

    @Mock
    private SpringDataChunkRepository chunkRepository;

    private JpaDocumentRepository repository;

    private static final UUID TEST_DOCUMENT_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final UUID TEST_CHUNK_ID = UUID.fromString("223e4567-e89b-12d3-a456-426614174001");

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        repository = new JpaDocumentRepository(documentRepository, chunkRepository, objectMapper);
    }

    @Nested
    @DisplayName("save(Document)")
    class SaveDocument {

        @Test
        @DisplayName("should save document and return domain object")
        void shouldSaveDocumentAndReturnDomainObject() {
            // Arrange
            Document document = createTestDocument(DocumentStatus.UPLOADING);
            DocumentEntity savedEntity = createTestEntity(DocumentEntity.DocumentStatus.UPLOADING);
            when(documentRepository.save(any(DocumentEntity.class))).thenReturn(savedEntity);

            // Act
            Document result = repository.save(document);

            // Assert
            assertThat(result).isNotNull();
            verify(documentRepository).save(any(DocumentEntity.class));
        }

        @Test
        @DisplayName("should map status to entity correctly")
        void shouldMapStatusToEntityCorrectly() {
            // Arrange
            Document document = createTestDocument(DocumentStatus.PROCESSING);
            DocumentEntity savedEntity = createTestEntity(DocumentEntity.DocumentStatus.PROCESSING);
            when(documentRepository.save(any(DocumentEntity.class))).thenReturn(savedEntity);
            ArgumentCaptor<DocumentEntity> entityCaptor = ArgumentCaptor.forClass(DocumentEntity.class);

            // Act
            repository.save(document);

            // Assert
            verify(documentRepository).save(entityCaptor.capture());
            assertThat(entityCaptor.getValue().getStatus()).isEqualTo(DocumentEntity.DocumentStatus.PROCESSING);
        }

        @Test
        @DisplayName("should map all document fields to entity")
        void shouldMapAllDocumentFieldsToEntity() {
            // Arrange
            Document document = createTestDocument(DocumentStatus.READY);
            DocumentEntity savedEntity = createTestEntity(DocumentEntity.DocumentStatus.READY);
            when(documentRepository.save(any(DocumentEntity.class))).thenReturn(savedEntity);
            ArgumentCaptor<DocumentEntity> entityCaptor = ArgumentCaptor.forClass(DocumentEntity.class);

            // Act
            repository.save(document);

            // Assert
            verify(documentRepository).save(entityCaptor.capture());
            DocumentEntity captured = entityCaptor.getValue();
            assertThat(captured.getId()).isEqualTo(TEST_DOCUMENT_ID);
            assertThat(captured.getTitle()).isEqualTo("Test Document");
            assertThat(captured.getFileName()).isEqualTo("test.pdf");
            assertThat(captured.getFileSize()).isEqualTo(1024L);
        }
    }

    @Nested
    @DisplayName("findById(UUID)")
    class FindById {

        @Test
        @DisplayName("should return document when found")
        void shouldReturnDocumentWhenFound() {
            // Arrange
            DocumentEntity entity = createTestEntity(DocumentEntity.DocumentStatus.READY);
            when(documentRepository.findById(TEST_DOCUMENT_ID)).thenReturn(Optional.of(entity));

            // Act
            Optional<Document> result = repository.findById(TEST_DOCUMENT_ID);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getId().value()).isEqualTo(TEST_DOCUMENT_ID);
        }

        @Test
        @DisplayName("should return empty when document not found")
        void shouldReturnEmptyWhenDocumentNotFound() {
            // Arrange
            when(documentRepository.findById(TEST_DOCUMENT_ID)).thenReturn(Optional.empty());

            // Act
            Optional<Document> result = repository.findById(TEST_DOCUMENT_ID);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should map entity status to domain status")
        void shouldMapEntityStatusToDomainStatus() {
            // Arrange
            DocumentEntity entity = createTestEntity(DocumentEntity.DocumentStatus.FAILED);
            when(documentRepository.findById(TEST_DOCUMENT_ID)).thenReturn(Optional.of(entity));

            // Act
            Optional<Document> result = repository.findById(TEST_DOCUMENT_ID);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getStatus()).isEqualTo(DocumentStatus.FAILED);
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should return all documents")
        void shouldReturnAllDocuments() {
            // Arrange
            List<DocumentEntity> entities = List.of(
                    createTestEntity(DocumentEntity.DocumentStatus.READY),
                    createTestEntity(DocumentEntity.DocumentStatus.PROCESSING)
            );
            when(documentRepository.findAll()).thenReturn(entities);

            // Act
            List<Document> results = repository.findAll();

            // Assert
            assertThat(results).hasSize(2);
        }

        @Test
        @DisplayName("should return empty list when no documents")
        void shouldReturnEmptyListWhenNoDocuments() {
            // Arrange
            when(documentRepository.findAll()).thenReturn(List.of());

            // Act
            List<Document> results = repository.findAll();

            // Assert
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("delete(UUID)")
    class DeleteDocument {

        @Test
        @DisplayName("should delete chunks and then document")
        void shouldDeleteChunksAndThenDocument() {
            // Act
            repository.delete(TEST_DOCUMENT_ID);

            // Assert
            var inOrder = inOrder(chunkRepository, documentRepository);
            inOrder.verify(chunkRepository).deleteByDocumentId(TEST_DOCUMENT_ID);
            inOrder.verify(documentRepository).deleteById(TEST_DOCUMENT_ID);
        }
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
            assertThat(saved.getId()).isEqualTo(TEST_CHUNK_ID);
            assertThat(saved.getDocumentId()).isEqualTo(TEST_DOCUMENT_ID);
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
            DocumentChunk chunk = new DocumentChunk(
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
            DocumentChunk chunk = new DocumentChunk(
                    TEST_CHUNK_ID,
                    TEST_DOCUMENT_ID,
                    "Test content",
                    0,
                    Map.of()
            ); // No embedding
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
            when(chunkRepository.findByDocumentId(TEST_DOCUMENT_ID)).thenReturn(List.of(entity));

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
            when(chunkRepository.findByDocumentId(TEST_DOCUMENT_ID)).thenReturn(List.of());

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
            verify(chunkRepository).deleteByDocumentId(TEST_DOCUMENT_ID);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle all domain status values when mapping to entity")
        void shouldHandleAllDomainStatusValuesWhenMappingToEntity() {
            for (DocumentStatus status : DocumentStatus.values()) {
                Document document = createTestDocument(status);
                DocumentEntity savedEntity = createTestEntityWithStatus(
                    DocumentEntity.DocumentStatus.valueOf(status.name())
                );
                when(documentRepository.save(any(DocumentEntity.class))).thenReturn(savedEntity);

                Document result = repository.save(document);

                assertThat(result).isNotNull();
                assertThat(result.getStatus()).isEqualTo(status);
            }
        }

        @Test
        @DisplayName("should handle all entity status values when mapping to domain")
        void shouldHandleAllEntityStatusValuesWhenMappingToDomain() {
            for (DocumentEntity.DocumentStatus status : DocumentEntity.DocumentStatus.values()) {
                DocumentEntity entity = createTestEntityWithStatus(status);
                when(documentRepository.findById(TEST_DOCUMENT_ID)).thenReturn(Optional.of(entity));

                Optional<Document> result = repository.findById(TEST_DOCUMENT_ID);

                assertThat(result).isPresent();
                assertThat(result.get().getStatus().name()).isEqualTo(status.name());
            }
        }

        @Test
        @DisplayName("should handle null metadata when deserializing chunk")
        void shouldHandleNullMetadataWhenDeserializingChunk() {
            DocumentChunkEntity entity = new DocumentChunkEntity(
                    TEST_CHUNK_ID,
                    TEST_DOCUMENT_ID,
                    "Content without metadata",
                    0,
                    new Float[]{0.1f, 0.2f},
                    null,
                    Instant.now()
            );
            when(chunkRepository.findByDocumentId(TEST_DOCUMENT_ID)).thenReturn(List.of(entity));

            List<DocumentChunk> results = repository.findChunksByDocumentId(TEST_DOCUMENT_ID);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getMetadata()).isEmpty();
        }

        @Test
        @DisplayName("should handle empty metadata when deserializing chunk")
        void shouldHandleEmptyMetadataWhenDeserializingChunk() {
            DocumentChunkEntity entity = new DocumentChunkEntity(
                    TEST_CHUNK_ID,
                    TEST_DOCUMENT_ID,
                    "Content with empty metadata",
                    0,
                    null,
                    "",
                    Instant.now()
            );
            when(chunkRepository.findByDocumentId(TEST_DOCUMENT_ID)).thenReturn(List.of(entity));

            List<DocumentChunk> results = repository.findChunksByDocumentId(TEST_DOCUMENT_ID);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getMetadata()).isEmpty();
        }

        @Test
        @DisplayName("should handle null embedding when finding chunks")
        void shouldHandleNullEmbeddingWhenFindingChunks() {
            DocumentChunkEntity entity = new DocumentChunkEntity(
                    TEST_CHUNK_ID,
                    TEST_DOCUMENT_ID,
                    "Content",
                    0,
                    null,
                    "{}",
                    Instant.now()
            );
            when(chunkRepository.findByDocumentId(TEST_DOCUMENT_ID)).thenReturn(List.of(entity));

            List<DocumentChunk> results = repository.findChunksByDocumentId(TEST_DOCUMENT_ID);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getEmbedding()).isNull();
        }

        @Test
        @DisplayName("should handle invalid metadata JSON gracefully")
        void shouldHandleInvalidMetadataJsonGracefully() {
            DocumentChunkEntity entity = new DocumentChunkEntity(
                    TEST_CHUNK_ID,
                    TEST_DOCUMENT_ID,
                    "Content",
                    0,
                    null,
                    "invalid json {",
                    Instant.now()
            );
            when(chunkRepository.findByDocumentId(TEST_DOCUMENT_ID)).thenReturn(List.of(entity));

            List<DocumentChunk> results = repository.findChunksByDocumentId(TEST_DOCUMENT_ID);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getMetadata()).isEmpty();
        }

        @Test
        @DisplayName("should handle empty metadata map when saving chunk")
        void shouldHandleEmptyMetadataMapWhenSavingChunk() {
            DocumentChunk chunk = new DocumentChunk(
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

    private DocumentEntity createTestEntity(DocumentEntity.DocumentStatus status) {
        return createTestEntityWithStatus(status);
    }

    private DocumentEntity createTestEntityWithStatus(DocumentEntity.DocumentStatus status) {
        return new DocumentEntity(
                TEST_DOCUMENT_ID,
                "Test Document",
                "test.pdf",
                1024L,
                status,
                Instant.now(),
                Instant.now()
        );
    }

    private Document createTestDocument(DocumentStatus status) {
        return new Document(
                DocumentId.of(TEST_DOCUMENT_ID),
                "Test Document",
                "test.pdf",
                1024L,
                status,
                Instant.now(),
                Instant.now()
        );
    }

    private DocumentChunk createTestChunk() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "test");
        
        return new DocumentChunk(
                TEST_CHUNK_ID,
                TEST_DOCUMENT_ID,
                "Test chunk content",
                0,
                metadata
        ).withEmbedding(new float[]{0.1f, 0.2f, 0.3f});
    }

    private DocumentChunkEntity createTestChunkEntity() {
        return new DocumentChunkEntity(
                TEST_CHUNK_ID,
                TEST_DOCUMENT_ID,
                "Test chunk content",
                0,
                new Float[]{0.1f, 0.2f, 0.3f},
                "{\"source\":\"test\"}",
                Instant.now()
        );
    }
}
