package com.ai.application.usecase;

import com.ai.application.port.DocumentRepositoryPort;
import com.ai.domain.exception.DocumentNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * DeleteDocumentUseCase Unit Tests
 * 
 * Tests using Mockito to mock external dependencies (ports):
 * - Naming convention: should_expected_result_when_condition
 * - Uses AAA pattern (Arrange-Act-Assert)
 * - Tests deletion flow and error handling
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteDocumentUseCase")
class DeleteDocumentUseCaseTest {

    @Mock
    private DocumentRepositoryPort documentRepository;

    private DeleteDocumentUseCase useCase;

    private static final UUID TEST_DOCUMENT_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @BeforeEach
    void setUp() {
        useCase = new DeleteDocumentUseCase(documentRepository);
    }

    @Nested
    @DisplayName("execute")
    class Execute {

        @Test
        @DisplayName("should delete document from repository when document exists")
        void shouldDeleteDocumentFromRepositoryWhenDocumentExists() {
            // Arrange
            when(documentRepository.findById(TEST_DOCUMENT_ID)).thenReturn(Optional.of(mock(com.ai.domain.model.Document.class)));

            // Act
            useCase.execute(TEST_DOCUMENT_ID);

            // Assert
            verify(documentRepository).delete(TEST_DOCUMENT_ID);
        }

        @Test
        @DisplayName("should delete all chunks from vector store")
        void shouldDeleteAllChunksFromVectorStore() {
            // Arrange
            when(documentRepository.findById(TEST_DOCUMENT_ID)).thenReturn(Optional.of(mock(com.ai.domain.model.Document.class)));

            // Act
            useCase.execute(TEST_DOCUMENT_ID);

            // Assert
            verify(documentRepository).deleteChunksByDocumentId(TEST_DOCUMENT_ID);
        }

        @Test
        @DisplayName("should throw DocumentNotFoundException when document doesn't exist")
        void shouldThrowDocumentNotFoundExceptionWhenDocumentDoesntExist() {
            // Arrange
            when(documentRepository.findById(TEST_DOCUMENT_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> useCase.execute(TEST_DOCUMENT_ID))
                    .isInstanceOf(DocumentNotFoundException.class)
                    .hasMessageContaining(TEST_DOCUMENT_ID.toString());

            // Verify delete was never called
            verify(documentRepository, never()).delete(any(UUID.class));
            verify(documentRepository, never()).deleteChunksByDocumentId(any(UUID.class));
        }

        @Test
        @DisplayName("should check document existence before deletion")
        void shouldCheckDocumentExistenceBeforeDeletion() {
            // Arrange
            when(documentRepository.findById(TEST_DOCUMENT_ID)).thenReturn(Optional.of(mock(com.ai.domain.model.Document.class)));

            // Act
            useCase.execute(TEST_DOCUMENT_ID);

            // Assert
            verify(documentRepository).findById(TEST_DOCUMENT_ID);
        }

        @Test
        @DisplayName("should delete chunks before deleting document")
        void shouldDeleteChunksBeforeDeletingDocument() {
            // Arrange
            when(documentRepository.findById(TEST_DOCUMENT_ID)).thenReturn(Optional.of(mock(com.ai.domain.model.Document.class)));

            // Act
            useCase.execute(TEST_DOCUMENT_ID);

            // Assert - verify order of operations
            var inOrder = inOrder(documentRepository);
            inOrder.verify(documentRepository).findById(TEST_DOCUMENT_ID);
            inOrder.verify(documentRepository).deleteChunksByDocumentId(TEST_DOCUMENT_ID);
            inOrder.verify(documentRepository).delete(TEST_DOCUMENT_ID);
        }

        @Test
        @DisplayName("should handle deletion of document with UUID")
        void shouldHandleDeletionOfDocumentWithUUID() {
            // Arrange
            UUID randomUUID = UUID.randomUUID();
            when(documentRepository.findById(randomUUID)).thenReturn(Optional.of(mock(com.ai.domain.model.Document.class)));

            // Act
            useCase.execute(randomUUID);

            // Assert
            verify(documentRepository).delete(randomUUID);
            verify(documentRepository).deleteChunksByDocumentId(randomUUID);
        }
    }

    @Nested
    @DisplayName("Exception Handling")
    class ExceptionHandling {

        @Test
        @DisplayName("should not attempt deletion after exception")
        void shouldNotAttemptDeletionAfterException() {
            // Arrange
            when(documentRepository.findById(TEST_DOCUMENT_ID)).thenReturn(Optional.empty());

            // Act & Assert
            try {
                useCase.execute(TEST_DOCUMENT_ID);
            } catch (DocumentNotFoundException expected) {
                // Expected exception
            }

            // Verify no deletion operations occurred
            verify(documentRepository, never()).delete(any(UUID.class));
            verify(documentRepository, never()).deleteChunksByDocumentId(any(UUID.class));
        }

        @Test
        @DisplayName("should propagate DocumentNotFoundException with correct message")
        void shouldPropagateDocumentNotFoundExceptionWithCorrectMessage() {
            // Arrange
            when(documentRepository.findById(TEST_DOCUMENT_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> useCase.execute(TEST_DOCUMENT_ID))
                    .isInstanceOf(DocumentNotFoundException.class)
                    .hasMessageContaining("Document not found");
        }
    }
}
