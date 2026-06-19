package com.ai.domain.service;

import com.ai.adapter.out.embedding.EmbeddingAdapter;
import com.ai.adapter.out.vector.PgVectorAdapter;
import com.ai.domain.model.Document;
import com.ai.domain.model.DocumentChunk;
import com.ai.domain.model.SourceDocument;
import com.ai.domain.repository.DocumentRepository;
import com.ai.domain.vo.DocumentId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RagService Unit Tests
 *
 * Tests for RAG (Retrieval-Augmented Generation) service following TDD principles:
 * - Naming convention: should_expected_result_when_condition
 * - Uses AAA pattern (Arrange-Act-Assert)
 * - Pure unit tests with mocked dependencies
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RagService")
class RagServiceTest {

    @Mock
    private EmbeddingAdapter embeddingAdapter;

    @Mock
    private PgVectorAdapter vectorAdapter;

    @Mock
    private ChunkingService chunkingService;

    @Mock
    private DocumentRepository documentRepository;

    private RagService ragService;

    private static final int EMBEDDING_DIMENSIONS = 384;

    @BeforeEach
    void setUp() {
        ragService = new RagService(
                chunkingService,
                embeddingAdapter,
                vectorAdapter,
                documentRepository
        );
        lenient().when(embeddingAdapter.getDimensions()).thenReturn(EMBEDDING_DIMENSIONS);
    }

    // ============ Document Upload Tests ============

    @Nested
    @DisplayName("uploadDocument")
    class UploadDocument {

        @Test
        @DisplayName("should upload document successfully")
        void shouldUploadDocumentSuccessfully() {
            // Arrange
            String title = "Test Document";
            String fileName = "test.txt";
            Long fileSize = 100L;
            String content = "This is a short document.";
            float[] mockEmbedding = createMockEmbedding();

            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
            when(chunkingService.chunk(content)).thenReturn(List.of(content));
            when(embeddingAdapter.embed(anyString())).thenReturn(mockEmbedding);
            doNothing().when(vectorAdapter).saveChunk(any(DocumentChunk.class));

            // Act
            Document result = ragService.uploadDocument(title, fileName, fileSize, content);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo(title);
            assertThat(result.getFileName()).isEqualTo(fileName);
            assertThat(result.getFileSize()).isEqualTo(fileSize);
            verify(documentRepository, times(2)).save(any(Document.class));
            verify(embeddingAdapter).embed(content);
            verify(vectorAdapter, atLeastOnce()).saveChunk(any(DocumentChunk.class));
        }

        @Test
        @DisplayName("should throw exception when embedding fails")
        void shouldThrowExceptionWhenEmbeddingFails() {
            // Arrange
            String content = "Short content.";
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
            when(chunkingService.chunk(content)).thenReturn(List.of(content));
            when(embeddingAdapter.embed(anyString())).thenThrow(new RuntimeException("Embedding failed"));
            doNothing().when(vectorAdapter).saveChunk(any(DocumentChunk.class));

            // Act & Assert
            assertThatThrownBy(() -> ragService.uploadDocument("Title", "file.txt", 100L, content))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to process document");
        }

        @Test
        @DisplayName("should mark document as failed when processing fails")
        void shouldMarkDocumentAsFailedWhenProcessingFails() {
            // Arrange
            String content = "Short content.";
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
            when(chunkingService.chunk(content)).thenReturn(List.of(content));
            when(embeddingAdapter.embed(anyString())).thenThrow(new RuntimeException("Embedding failed"));

            // Act
            try {
                ragService.uploadDocument("Title", "file.txt", 100L, content);
            } catch (Exception ignored) {}

            // Assert
            ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
            verify(documentRepository, times(2)).save(documentCaptor.capture());
            Document failedDocument = documentCaptor.getAllValues().get(1);
            assertThat(failedDocument.getStatus().name()).isEqualTo("FAILED");
        }

        @Test
        @DisplayName("should throw exception when vector save fails")
        void shouldThrowExceptionWhenVectorSaveFails() {
            // Arrange
            String content = "Short content for testing.";
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
            when(chunkingService.chunk(content)).thenReturn(List.of(content));
            when(embeddingAdapter.embed(anyString())).thenReturn(createMockEmbedding());
            doThrow(new RuntimeException("Vector store error")).when(vectorAdapter).saveChunk(any(DocumentChunk.class));

            // Act & Assert
            assertThatThrownBy(() -> ragService.uploadDocument("Title", "file.txt", 100L, content))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to process document");
        }

        @Test
        @DisplayName("should include metadata in chunks")
        void shouldIncludeMetadataInChunks() {
            // Arrange
            String title = "My Document";
            String fileName = "doc.pdf";
            String content = "Short content.";
            float[] mockEmbedding = createMockEmbedding();

            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
            when(chunkingService.chunk(content)).thenReturn(List.of(content));
            when(embeddingAdapter.embed(anyString())).thenReturn(mockEmbedding);

            ArgumentCaptor<DocumentChunk> chunkCaptor = ArgumentCaptor.forClass(DocumentChunk.class);
            doNothing().when(vectorAdapter).saveChunk(chunkCaptor.capture());

            // Act
            ragService.uploadDocument(title, fileName, 100L, content);

            // Assert
            DocumentChunk chunk = chunkCaptor.getValue();
            assertThat(chunk.getMetadata()).containsEntry("title", title);
            assertThat(chunk.getMetadata()).containsEntry("fileName", fileName);
        }
    }

    // ============ List Documents Tests ============

    @Nested
    @DisplayName("listDocuments")
    class ListDocuments {

        @Test
        @DisplayName("should return all documents")
        void shouldReturnAllDocuments() {
            // Arrange
            Document doc1 = new Document(DocumentId.generate(), "Doc 1", "file1.txt", 100L);
            Document doc2 = new Document(DocumentId.generate(), "Doc 2", "file2.txt", 200L);
            when(documentRepository.findAll()).thenReturn(List.of(doc1, doc2));

            // Act
            List<Document> result = ragService.listDocuments();

            // Assert
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("should return empty list when no documents")
        void shouldReturnEmptyListWhenNoDocuments() {
            // Arrange
            when(documentRepository.findAll()).thenReturn(List.of());

            // Act
            List<Document> result = ragService.listDocuments();

            // Assert
            assertThat(result).isEmpty();
        }
    }

    // ============ Delete Document Tests ============

    @Nested
    @DisplayName("deleteDocument")
    class DeleteDocument {

        @Test
        @DisplayName("should delete document successfully")
        void shouldDeleteDocumentSuccessfully() {
            // Arrange
            UUID documentId = UUID.randomUUID();
            Document document = new Document(DocumentId.of(documentId), "Doc", "file.txt", 100L);
            when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
            doNothing().when(documentRepository).delete(documentId);
            // Stub both overloaded methods
            when(vectorAdapter.search(any(), anyInt())).thenReturn(List.of());
            when(vectorAdapter.search(any(), anyInt(), any())).thenReturn(List.of());

            // Act
            ragService.deleteDocument(documentId);

            // Assert
            verify(documentRepository).delete(documentId);
        }

        @Test
        @DisplayName("should throw exception when document not found")
        void shouldThrowExceptionWhenDocumentNotFound() {
            // Arrange
            UUID documentId = UUID.randomUUID();
            when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> ragService.deleteDocument(documentId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Document not found");
        }
    }

    // ============ Retrieve Context Tests ============

    @Nested
    @DisplayName("retrieveContext")
    class RetrieveContext {

        @Test
        @DisplayName("should retrieve context with results")
        void shouldRetrieveContextWithResults() {
            // Arrange
            String query = "What is AI?";
            float[] mockEmbedding = createMockEmbedding();
            float[] chunkEmbedding = createMockEmbedding();

            DocumentChunk chunk = new DocumentChunk(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "AI is Artificial Intelligence.",
                    0,
                    Map.of("title", "Test Doc")
            ).withEmbedding(chunkEmbedding);

            when(embeddingAdapter.embed(query)).thenReturn(mockEmbedding);
            // Stub both overloaded methods - the 2-arg version delegates to 3-arg
            when(vectorAdapter.search(any(), anyInt())).thenReturn(List.of(chunk));
            when(vectorAdapter.search(any(), anyInt(), any())).thenReturn(List.of(chunk));

            // Act
            RagService.RetrievalResult result = ragService.retrieveContext(query, null, 5);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.context()).contains("AI is Artificial Intelligence");
            assertThat(result.sources()).hasSize(1);
            assertThat(result.enrichedQuery()).isEqualTo(query);
        }

        @Test
        @DisplayName("should return empty context when no chunks found")
        void shouldReturnEmptyContextWhenNoChunksFound() {
            // Arrange
            String query = "No results query";
            float[] mockEmbedding = createMockEmbedding();

            when(embeddingAdapter.embed(query)).thenReturn(mockEmbedding);
            // Stub both overloaded methods
            when(vectorAdapter.search(any(), anyInt())).thenReturn(List.of());
            when(vectorAdapter.search(any(), anyInt(), any())).thenReturn(List.of());

            // Act
            RagService.RetrievalResult result = ragService.retrieveContext(query, null, 5);

            // Assert
            assertThat(result.context()).isEmpty();
            assertThat(result.sources()).isEmpty();
        }

        @Test
        @DisplayName("should filter by document IDs when provided")
        void shouldFilterByDocumentIdsWhenProvided() {
            // Arrange
            String query = "Test query";
            UUID docId = UUID.randomUUID();
            List<UUID> docIds = List.of(docId);

            when(embeddingAdapter.embed(query)).thenReturn(createMockEmbedding());
            when(vectorAdapter.search(any(float[].class), anyInt(), any())).thenReturn(List.of());

            // Act
            RagService.RetrievalResult result = ragService.retrieveContext(query, docIds, 5);

            // Assert
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should handle null docIds")
        void shouldHandleNullDocIds() {
            // Arrange
            when(embeddingAdapter.embed(anyString())).thenReturn(createMockEmbedding());
            when(vectorAdapter.search(any(float[].class), anyInt(), any())).thenReturn(List.of());

            // Act
            RagService.RetrievalResult result = ragService.retrieveContext("query", null, 5);

            // Assert
            assertThat(result).isNotNull();
        }
    }

    // ============ RetrievalResult Record Tests ============

    @Nested
    @DisplayName("RetrievalResult record")
    class RetrievalResultTests {

        @Test
        @DisplayName("should create RetrievalResult with all fields")
        void shouldCreateRetrievalResultWithAllFields() {
            // Act
            RagService.RetrievalResult result = new RagService.RetrievalResult(
                    "context",
                    List.of(new SourceDocument("source", 0.9, Map.of())),
                    "query"
            );

            // Assert
            assertThat(result.context()).isEqualTo("context");
            assertThat(result.sources()).hasSize(1);
            assertThat(result.enrichedQuery()).isEqualTo("query");
        }

        @Test
        @DisplayName("should support equals and hashCode")
        void shouldSupportEqualsAndHashCode() {
            // Arrange
            RagService.RetrievalResult result1 = new RagService.RetrievalResult(
                    "ctx", List.of(), "q"
            );
            RagService.RetrievalResult result2 = new RagService.RetrievalResult(
                    "ctx", List.of(), "q"
            );

            // Assert
            assertThat(result1).isEqualTo(result2);
            assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when different context")
        void shouldNotBeEqualWhenDifferentContext() {
            // Arrange
            RagService.RetrievalResult result1 = new RagService.RetrievalResult(
                    "ctx1", List.of(), "q"
            );
            RagService.RetrievalResult result2 = new RagService.RetrievalResult(
                    "ctx2", List.of(), "q"
            );

            // Assert
            assertThat(result1).isNotEqualTo(result2);
        }
    }

    // ============ Helper Methods ============

    private float[] createMockEmbedding() {
        float[] embedding = new float[EMBEDDING_DIMENSIONS];
        Arrays.fill(embedding, 0.1f);
        return embedding;
    }
}
