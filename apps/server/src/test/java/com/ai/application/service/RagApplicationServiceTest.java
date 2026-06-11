package com.ai.application.service;

import com.ai.application.port.DocumentRepositoryPort;
import com.ai.application.usecase.DeleteDocumentUseCase;
import com.ai.application.usecase.RagChatUseCase;
import com.ai.application.usecase.UploadDocumentUseCase;
import com.ai.domain.model.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * RagApplicationService Unit Tests
 * 
 * Tests using Mockito to mock use case dependencies:
 * - Naming convention: should_expected_result_when_condition
 * - Uses AAA pattern (Arrange-Act-Assert)
 * - Tests service facade delegation
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RagApplicationService")
class RagApplicationServiceTest {

    @Mock
    private UploadDocumentUseCase uploadUseCase;

    @Mock
    private DeleteDocumentUseCase deleteUseCase;

    @Mock
    private RagChatUseCase ragChatUseCase;

    @Mock
    private DocumentRepositoryPort documentRepository;

    private RagApplicationService service;

    private static final String TEST_TITLE = "Test Document";
    private static final String TEST_FILE_NAME = "test.txt";
    private static final Long TEST_FILE_SIZE = 1024L;
    private static final String TEST_CONTENT = "Test content";
    private static final UUID TEST_DOCUMENT_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @BeforeEach
    void setUp() {
        service = new RagApplicationService(
            uploadUseCase,
            deleteUseCase,
            ragChatUseCase,
            documentRepository
        );
    }

    @Nested
    @DisplayName("uploadDocument")
    class UploadDocument {

        @Test
        @DisplayName("should delegate upload to use case")
        void shouldDelegateUploadToUseCase() {
            // Arrange
            Document expectedDocument = mock(Document.class);
            when(uploadUseCase.execute(TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE, TEST_CONTENT))
                .thenReturn(expectedDocument);

            // Act
            Document result = service.uploadDocument(TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE, TEST_CONTENT);

            // Assert
            assertThat(result).isEqualTo(expectedDocument);
            verify(uploadUseCase).execute(TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE, TEST_CONTENT);
        }

        @Test
        @DisplayName("should pass all parameters to use case")
        void shouldPassAllParametersToUseCase() {
            // Arrange
            when(uploadUseCase.execute(anyString(), anyString(), anyLong(), anyString()))
                .thenReturn(mock(Document.class));

            // Act
            service.uploadDocument("Custom Title", "custom.pdf", 2048L, "Custom content");

            // Assert
            verify(uploadUseCase).execute("Custom Title", "custom.pdf", 2048L, "Custom content");
        }
    }

    @Nested
    @DisplayName("deleteDocument")
    class DeleteDocument {

        @Test
        @DisplayName("should delegate delete to use case")
        void shouldDelegateDeleteToUseCase() {
            // Arrange
            doNothing().when(deleteUseCase).execute(TEST_DOCUMENT_ID);

            // Act
            service.deleteDocument(TEST_DOCUMENT_ID);

            // Assert
            verify(deleteUseCase).execute(TEST_DOCUMENT_ID);
        }

        @Test
        @DisplayName("should pass correct document ID to use case")
        void shouldPassCorrectDocumentIdToUseCase() {
            // Arrange
            UUID customId = UUID.randomUUID();
            doNothing().when(deleteUseCase).execute(customId);

            // Act
            service.deleteDocument(customId);

            // Assert
            verify(deleteUseCase).execute(customId);
        }
    }

    @Nested
    @DisplayName("retrieveContext")
    class RetrieveContext {

        @Test
        @DisplayName("should delegate rag chat to use case")
        void shouldDelegateRagChatToUseCase() {
            // Arrange
            String query = "What is AI?";
            List<UUID> docIds = List.of(TEST_DOCUMENT_ID);
            int topK = 5;
            
            RagChatUseCase.RetrievalResult expectedResult = mock(RagChatUseCase.RetrievalResult.class);
            when(ragChatUseCase.execute(query, docIds, topK)).thenReturn(expectedResult);

            // Act
            RagChatUseCase.RetrievalResult result = service.retrieveContext(query, docIds, topK);

            // Assert
            assertThat(result).isEqualTo(expectedResult);
            verify(ragChatUseCase).execute(query, docIds, topK);
        }

        @Test
        @DisplayName("should pass all parameters to use case")
        void shouldPassAllParametersToUseCase() {
            // Arrange
            String customQuery = "Custom query";
            List<UUID> customDocIds = List.of(UUID.randomUUID());
            int customTopK = 10;
            
            RagChatUseCase.RetrievalResult mockResult = mock(RagChatUseCase.RetrievalResult.class);
            when(ragChatUseCase.execute(anyString(), any(), anyInt())).thenReturn(mockResult);

            // Act
            service.retrieveContext(customQuery, customDocIds, customTopK);

            // Assert
            verify(ragChatUseCase).execute(customQuery, customDocIds, customTopK);
        }

        @Test
        @DisplayName("should handle null docIds")
        void shouldHandleNullDocIds() {
            // Arrange
            RagChatUseCase.RetrievalResult mockResult = mock(RagChatUseCase.RetrievalResult.class);
            when(ragChatUseCase.execute(anyString(), isNull(), anyInt())).thenReturn(mockResult);

            // Act
            service.retrieveContext("Query", null, 5);

            // Assert
            verify(ragChatUseCase).execute("Query", null, 5);
        }
    }

    @Nested
    @DisplayName("listDocuments")
    class ListDocuments {

        @Test
        @DisplayName("should return documents from repository")
        void shouldReturnDocumentsFromRepository() {
            // Arrange
            Document doc1 = mock(Document.class);
            Document doc2 = mock(Document.class);
            List<Document> expectedDocs = List.of(doc1, doc2);
            when(documentRepository.findAll()).thenReturn(expectedDocs);

            // Act
            List<Document> result = service.listDocuments();

            // Assert
            assertThat(result).isEqualTo(expectedDocs);
            verify(documentRepository).findAll();
        }

        @Test
        @DisplayName("should return empty list when no documents")
        void shouldReturnEmptyListWhenNoDocuments() {
            // Arrange
            when(documentRepository.findAll()).thenReturn(List.of());

            // Act
            List<Document> result = service.listDocuments();

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return multiple documents")
        void shouldReturnMultipleDocuments() {
            // Arrange
            List<Document> expectedDocs = List.of(
                mock(Document.class),
                mock(Document.class),
                mock(Document.class)
            );
            when(documentRepository.findAll()).thenReturn(expectedDocs);

            // Act
            List<Document> result = service.listDocuments();

            // Assert
            assertThat(result).hasSize(3);
        }
    }

    @Nested
    @DisplayName("Integration")
    class Integration {

        @Test
        @DisplayName("should allow multiple operations in sequence")
        void shouldAllowMultipleOperationsInSequence() {
            // Arrange
            Document uploadedDoc = mock(Document.class);
            when(uploadedDoc.getId()).thenReturn(TEST_DOCUMENT_ID);
            when(uploadUseCase.execute(anyString(), anyString(), anyLong(), anyString()))
                .thenReturn(uploadedDoc);
            doNothing().when(deleteUseCase).execute(any(UUID.class));
            when(documentRepository.findAll()).thenReturn(List.of(uploadedDoc));

            // Act
            Document doc = service.uploadDocument(TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE, TEST_CONTENT);
            service.deleteDocument(doc.getId());
            List<Document> docs = service.listDocuments();

            // Assert
            verify(uploadUseCase).execute(TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE, TEST_CONTENT);
            verify(deleteUseCase).execute(TEST_DOCUMENT_ID);
            verify(documentRepository).findAll();
        }

        @Test
        @DisplayName("should handle multiple document operations")
        void shouldHandleMultipleDocumentOperations() {
            // Arrange
            Document doc1 = mock(Document.class);
            when(doc1.getId()).thenReturn(UUID.randomUUID());
            Document doc2 = mock(Document.class);
            when(doc2.getId()).thenReturn(UUID.randomUUID());
            
            when(uploadUseCase.execute(eq("Doc 1"), anyString(), anyLong(), anyString())).thenReturn(doc1);
            when(uploadUseCase.execute(eq("Doc 2"), anyString(), anyLong(), anyString())).thenReturn(doc2);

            // Act
            Document result1 = service.uploadDocument("Doc 1", "file1.txt", 100L, "Content 1");
            Document result2 = service.uploadDocument("Doc 2", "file2.txt", 200L, "Content 2");

            // Assert
            assertThat(result1).isEqualTo(doc1);
            assertThat(result2).isEqualTo(doc2);
            verify(uploadUseCase, times(2)).execute(anyString(), anyString(), anyLong(), anyString());
        }
    }
}
