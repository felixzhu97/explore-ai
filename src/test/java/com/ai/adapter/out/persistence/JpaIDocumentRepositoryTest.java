package com.ai.adapter.out.persistence;

import com.ai.rag.domain.model.Document;
import com.ai.rag.domain.model.DocumentStatus;
import com.ai.rag.infrastructure.storage.SpringDataDocumentRepository;
import com.ai.rag.domain.vo.DocumentId;
import com.ai.rag.infrastructure.storage.DocumentEntity;
import com.ai.rag.infrastructure.storage.JpaIDocumentRepository;
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
 * JpaDocumentRepository Unit Tests
 * 
 * Tests using Mockito to mock external dependencies (Spring Data repositories):
 * - Naming convention: should_expected_result_when_condition
 * - Uses AAA pattern (Arrange-Act-Assert)
 * - Tests save/find operations and entity mapping
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaDocumentRepository")
class JpaIDocumentRepositoryTest {

    @Mock
    private SpringDataDocumentRepository documentRepository;

    private JpaIDocumentRepository repository;

    private static final UUID TEST_DOCUMENT_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @BeforeEach
    void setUp() {
        repository = new JpaIDocumentRepository(documentRepository);
    }

    @Nested
    @DisplayName("save(Document)")
    class SaveDocument {

        @Test
        @DisplayName("should save document and return domain object")
        void shouldSaveDocumentAndReturnDomainObject() {
            // Arrange
            Document document = createTestDocument(DocumentStatus.UPLOADING);
            DocumentEntity savedEntity = createTestEntity(DocumentStatus.UPLOADING);
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
            DocumentEntity savedEntity = createTestEntity(DocumentStatus.PROCESSING);
            when(documentRepository.save(any(DocumentEntity.class))).thenReturn(savedEntity);
            ArgumentCaptor<DocumentEntity> entityCaptor = ArgumentCaptor.forClass(DocumentEntity.class);

            // Act
            repository.save(document);

            // Assert
            verify(documentRepository).save(entityCaptor.capture());
            assertThat(entityCaptor.getValue().getStatus()).isEqualTo(DocumentStatus.PROCESSING);
        }

        @Test
        @DisplayName("should map all document fields to entity")
        void shouldMapAllDocumentFieldsToEntity() {
            // Arrange
            Document document = createTestDocument(DocumentStatus.READY);
            DocumentEntity savedEntity = createTestEntity(DocumentStatus.READY);
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
            DocumentEntity entity = createTestEntity(DocumentStatus.READY);
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
            DocumentEntity entity = createTestEntity(DocumentStatus.FAILED);
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
                    createTestEntity(DocumentStatus.READY),
                    createTestEntity(DocumentStatus.PROCESSING)
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
        @DisplayName("should delete document")
        void shouldDeleteDocument() {
            // Act
            repository.delete(TEST_DOCUMENT_ID);

            // Assert
            verify(documentRepository).deleteById(TEST_DOCUMENT_ID);
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
                DocumentEntity savedEntity = createTestEntityWithStatus(status);
                when(documentRepository.save(any(DocumentEntity.class))).thenReturn(savedEntity);

                Document result = repository.save(document);

                assertThat(result).isNotNull();
                assertThat(result.getStatus()).isEqualTo(status);
            }
        }

        @Test
        @DisplayName("should handle all entity status values when mapping to domain")
        void shouldHandleAllEntityStatusValuesWhenMappingToDomain() {
            for (DocumentStatus status : DocumentStatus.values()) {
                DocumentEntity entity = createTestEntityWithStatus(status);
                when(documentRepository.findById(TEST_DOCUMENT_ID)).thenReturn(Optional.of(entity));

                Optional<Document> result = repository.findById(TEST_DOCUMENT_ID);

                assertThat(result).isPresent();
                assertThat(result.get().getStatus().name()).isEqualTo(status.name());
            }
        }
    }

    private DocumentEntity createTestEntity(DocumentStatus status) {
        return createTestEntityWithStatus(status);
    }

    private DocumentEntity createTestEntityWithStatus(DocumentStatus status) {
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
}
