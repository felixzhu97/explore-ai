package com.ai.application.usecase;

import com.ai.application.port.DocumentRepositoryPort;
import com.ai.application.port.EmbeddingPort;
import com.ai.application.port.VectorSearchPort;
import com.ai.domain.model.Document;
import com.ai.domain.model.DocumentChunk;
import com.ai.domain.model.DocumentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * UploadDocumentUseCase Unit Tests
 * 
 * Tests using Mockito to mock external dependencies (ports):
 * - Naming convention: should_expected_result_when_condition
 * - Uses AAA pattern (Arrange-Act-Assert)
 * - Tests business flow and integration between ports
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UploadDocumentUseCase")
class UploadDocumentUseCaseTest {

    @Mock
    private DocumentRepositoryPort documentRepository;

    @Mock
    private EmbeddingPort embeddingPort;

    @Mock
    private VectorSearchPort vectorSearchPort;

    private UploadDocumentUseCase useCase;

    private static final String TEST_TITLE = "Test Document";
    private static final String TEST_FILE_NAME = "test.txt";
    private static final Long TEST_FILE_SIZE = 1024L;
    private static final String TEST_CONTENT = "This is test content. With multiple sentences. And some more text!";

    @BeforeEach
    void setUp() {
        useCase = new UploadDocumentUseCase(documentRepository, embeddingPort, vectorSearchPort, 500, 50);
    }

    @Nested
    @DisplayName("execute")
    class Execute {

        @Test
        @DisplayName("should save document and generate chunks")
        void shouldSaveDocumentAndGenerateChunks() {
            // Arrange
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
            when(embeddingPort.embed(any(String.class))).thenReturn(new float[]{0.1f, 0.2f});

            // Act
            Document result = useCase.execute(TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE, TEST_CONTENT);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo(TEST_TITLE);
            assertThat(result.getFileName()).isEqualTo(TEST_FILE_NAME);
            assertThat(result.getFileSize()).isEqualTo(TEST_FILE_SIZE);
            verify(documentRepository, atLeastOnce()).save(any(Document.class));
        }

        @Test
        @DisplayName("should call embedding service for each chunk")
        void shouldCallEmbeddingServiceForEachChunk() {
            // Arrange
            String content = "First sentence. Second sentence. Third sentence.";
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
            when(embeddingPort.embed(any(String.class))).thenReturn(new float[]{0.1f, 0.2f});

            // Act
            useCase.execute(TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE, content);

            // Assert - embedding should be called for each chunk
            verify(embeddingPort, atLeastOnce()).embed(any(String.class));
        }

        @Test
        @DisplayName("should upsert vectors for each chunk")
        void shouldUpsertVectorsForEachChunk() {
            // Arrange
            String content = "First sentence. Second sentence.";
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
            when(embeddingPort.embed(any(String.class))).thenReturn(new float[]{0.1f, 0.2f});

            // Act
            useCase.execute(TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE, content);

            // Assert - vectorSearchPort.saveChunk should be called for each chunk
            verify(vectorSearchPort, atLeastOnce()).saveChunk(any(DocumentChunk.class));
        }

        @Test
        @DisplayName("should set document status to READY after successful processing")
        void shouldSetDocumentStatusToReadyAfterSuccessfulProcessing() {
            // Arrange
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
            when(embeddingPort.embed(any(String.class))).thenReturn(new float[]{0.1f, 0.2f});

            // Act
            Document result = useCase.execute(TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE, TEST_CONTENT);

            // Assert
            assertThat(result.getStatus()).isEqualTo(DocumentStatus.READY);
        }

        @Test
        @DisplayName("should handle empty content gracefully")
        void shouldHandleEmptyContentGracefully() {
            // Arrange
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
            when(embeddingPort.embed(any(String.class))).thenReturn(new float[]{0.1f, 0.2f});

            // Act
            Document result = useCase.execute(TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE, "");

            // Assert - even empty content results in one chunk due to how chunkText works
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(DocumentStatus.READY);
        }

        @Test
        @DisplayName("should handle whitespace-only content gracefully")
        void shouldHandleWhitespaceOnlyContentGracefully() {
            // Arrange
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
            when(embeddingPort.embed(any(String.class))).thenReturn(new float[]{0.1f, 0.2f});

            // Act
            Document result = useCase.execute(TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE, "   \n\t  ");

            // Assert - whitespace content may still produce a chunk
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should mark document FAILED on embedding error")
        void shouldMarkDocumentFailedOnEmbeddingError() {
            // Arrange
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
            when(embeddingPort.embed(any(String.class)))
                    .thenThrow(new RuntimeException("Embedding service error"));

            // Act & Assert
            assertThatThrownBy(() -> useCase.execute(TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE, "Some content."))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to process document");

            // Verify the document was marked as FAILED
            ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
            verify(documentRepository, atLeast(2)).save(documentCaptor.capture());
            
            List<Document> savedDocuments = documentCaptor.getAllValues();
            Document lastSaved = savedDocuments.get(savedDocuments.size() - 1);
            assertThat(lastSaved.getStatus()).isEqualTo(DocumentStatus.FAILED);
        }

        @Test
        @DisplayName("should handle content without sentence separators")
        void shouldHandleContentWithoutSentenceSeparators() {
            // Arrange
            String content = "This is a long text without proper sentence separators but it should still be chunked";
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
            when(embeddingPort.embed(any(String.class))).thenReturn(new float[]{0.1f, 0.2f});

            // Act
            Document result = useCase.execute(TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE, content);

            // Assert
            assertThat(result.getStatus()).isEqualTo(DocumentStatus.READY);
        }

        @Test
        @DisplayName("should save document with unique ID")
        void shouldSaveDocumentWithUniqueId() {
            // Arrange
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
            when(embeddingPort.embed(any(String.class))).thenReturn(new float[]{0.1f});

            // Act
            Document result = useCase.execute(TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE, "Content.");

            // Assert
            assertThat(result.getId()).isNotNull();
        }

        @Test
        @DisplayName("should include metadata in chunks")
        void shouldIncludeMetadataInChunks() {
            // Arrange
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
            when(embeddingPort.embed(any(String.class))).thenReturn(new float[]{0.1f, 0.2f});

            // Act
            useCase.execute(TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE, "Sentence one. Sentence two.");

            // Assert
            ArgumentCaptor<DocumentChunk> chunkCaptor = ArgumentCaptor.forClass(DocumentChunk.class);
            verify(vectorSearchPort, atLeastOnce()).saveChunk(chunkCaptor.capture());

            List<DocumentChunk> savedChunks = chunkCaptor.getAllValues();
            assertThat(savedChunks).isNotEmpty();
            for (DocumentChunk chunk : savedChunks) {
                assertThat(chunk.getMetadata()).containsKey("title");
                assertThat(chunk.getMetadata()).containsKey("fileName");
            }
        }

        @Test
        @DisplayName("should set chunk indices correctly")
        void shouldSetChunkIndicesCorrectly() {
            // Arrange
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
            when(embeddingPort.embed(any(String.class))).thenReturn(new float[]{0.1f});

            // Act
            useCase.execute(TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE, "First. Second. Third.");

            // Assert
            ArgumentCaptor<DocumentChunk> chunkCaptor = ArgumentCaptor.forClass(DocumentChunk.class);
            verify(vectorSearchPort, atLeastOnce()).saveChunk(chunkCaptor.capture());

            List<DocumentChunk> chunks = chunkCaptor.getAllValues();
            for (int i = 0; i < chunks.size(); i++) {
                assertThat(chunks.get(i).getChunkIndex()).isEqualTo(i);
            }
        }

        @Test
        @DisplayName("should flush repository after saving document")
        void shouldFlushRepositoryAfterSavingDocument() {
            // Arrange
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
            when(embeddingPort.embed(any(String.class))).thenReturn(new float[]{0.1f});

            // Act
            useCase.execute(TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE, "Content.");

            // Assert - verify flush was called to ensure document is persisted before chunks
            verify(documentRepository).flush();
        }
    }

    @Nested
    @DisplayName("Text Chunking")
    class TextChunking {

        @Test
        @DisplayName("should split content into sentence-based chunks")
        void shouldSplitContentIntoSentenceBasedChunks() {
            // Arrange
            String content = "Sentence one. Sentence two. Sentence three.";
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
            when(embeddingPort.embed(any(String.class))).thenReturn(new float[]{0.1f});

            // Act
            useCase.execute(TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE, content);

            // Assert
            ArgumentCaptor<DocumentChunk> chunkCaptor = ArgumentCaptor.forClass(DocumentChunk.class);
            verify(vectorSearchPort, atLeastOnce()).saveChunk(chunkCaptor.capture());
            
            List<DocumentChunk> chunks = chunkCaptor.getAllValues();
            assertThat(chunks).hasSizeGreaterThan(0);
        }

        @Test
        @DisplayName("should handle multiple sentences in one chunk")
        void shouldHandleMultipleSentencesInOneChunk() {
            // Arrange
            String shortContent = "Short. Sentence.";
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
            when(embeddingPort.embed(any(String.class))).thenReturn(new float[]{0.1f});

            // Act
            useCase.execute(TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE, shortContent);

            // Assert
            ArgumentCaptor<DocumentChunk> chunkCaptor = ArgumentCaptor.forClass(DocumentChunk.class);
            verify(vectorSearchPort, atLeastOnce()).saveChunk(chunkCaptor.capture());
            
            List<DocumentChunk> chunks = chunkCaptor.getAllValues();
            // Should create at least one chunk containing multiple sentences
            assertThat(chunks).isNotEmpty();
        }
    }
}
