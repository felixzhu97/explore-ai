package com.ai.service;

import com.ai.domain.model.Document;
import com.ai.domain.model.DocumentChunk;
import com.ai.domain.model.DocumentStatus;
import com.ai.domain.model.SourceDocument;
import com.ai.domain.vo.DocumentId;
import com.ai.infrastructure.adapter.embedding.EmbeddingAdapter;
import com.ai.infrastructure.adapter.persistence.JpaDocumentRepository;
import com.ai.infrastructure.adapter.vector.PgVectorAdapter;
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
 * Tests using Mockito to mock external dependencies:
 * - Naming convention: should_expected_result_when_condition
 * - Uses AAA pattern (Arrange-Act-Assert)
 * - Tests upload, delete, list, and retrieve operations
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
    private JpaDocumentRepository documentRepository;

    private RagService ragService;

    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 50;
    private static final int EMBEDDING_DIMENSIONS = 768;

    @BeforeEach
    void setUp() {
        ragService = new RagService(
            CHUNK_SIZE,
            CHUNK_OVERLAP,
            embeddingAdapter,
            vectorAdapter,
            documentRepository
        );
    }

    @Nested
    @DisplayName("uploadDocument")
    class UploadDocument {

        @Test
        @DisplayName("should upload document and return READY status")
        void shouldUploadDocumentAndReturnReadyStatus() {
            // Arrange
            String title = "Test Document";
            String fileName = "test.txt";
            Long fileSize = 1024L;
            String content = "This is a test document with some content.";

            when(embeddingAdapter.embed(anyString())).thenReturn(new float[EMBEDDING_DIMENSIONS]);
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            Document result = ragService.uploadDocument(title, fileName, fileSize, content);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(DocumentStatus.READY);
            assertThat(result.getTitle()).isEqualTo(title);
            verify(vectorAdapter, atLeastOnce()).saveChunk(any(DocumentChunk.class));
        }

        @Test
        @DisplayName("should chunk text and create embeddings for each chunk")
        void shouldChunkTextAndCreateEmbeddingsForEachChunk() {
            // Arrange
            String content = "First sentence. Second sentence. Third sentence. Fourth sentence.";

            when(embeddingAdapter.embed(anyString())).thenReturn(new float[EMBEDDING_DIMENSIONS]);
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            Document result = ragService.uploadDocument("Title", "file.txt", 100L, content);

            // Assert
            verify(embeddingAdapter, atLeastOnce()).embed(anyString());
            verify(vectorAdapter, atLeastOnce()).saveChunk(any(DocumentChunk.class));
        }

        @Test
        @DisplayName("should mark document as PROCESSING initially then READY after success")
        void shouldMarkDocumentAsProcessingInitiallyThenReadyAfterSuccess() {
            // Arrange
            String content = "Test content for processing.";
            List<DocumentStatus> capturedStatuses = new ArrayList<>();

            when(embeddingAdapter.embed(anyString())).thenReturn(new float[EMBEDDING_DIMENSIONS]);
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
                Document doc = inv.getArgument(0);
                capturedStatuses.add(doc.getStatus());
                return doc;
            });

            // Act
            Document result = ragService.uploadDocument("Title", "file.txt", 100L, content);

            // Assert - At least one save was with PROCESSING status and final status is READY
            assertThat(capturedStatuses).contains(DocumentStatus.PROCESSING);
            assertThat(result.getStatus()).isEqualTo(DocumentStatus.READY);
        }

        @Test
        @DisplayName("should mark document as FAILED when vector storage fails")
        void shouldMarkDocumentAsFailedWhenVectorStorageFails() {
            // Arrange
            String content = "Test content.";

            when(embeddingAdapter.embed(anyString())).thenReturn(new float[EMBEDDING_DIMENSIONS]);
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
            doThrow(new RuntimeException("Vector storage error"))
                .when(vectorAdapter).saveChunk(any(DocumentChunk.class));

            // Act & Assert
            assertThatThrownBy(() -> ragService.uploadDocument("Title", "file.txt", 100L, content))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process document");

            // Verify document was marked as failed
            ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
            verify(documentRepository, atLeast(2)).save(docCaptor.capture());
            assertThat(docCaptor.getAllValues().get(docCaptor.getAllValues().size() - 1).getStatus())
                .isEqualTo(DocumentStatus.FAILED);
        }
    }

    @Nested
    @DisplayName("listDocuments")
    class ListDocuments {

        @Test
        @DisplayName("should return all documents from repository")
        void shouldReturnAllDocumentsFromRepository() {
            // Arrange
            Document doc1 = createTestDocument("Doc 1", DocumentStatus.READY);
            Document doc2 = createTestDocument("Doc 2", DocumentStatus.PROCESSING);
            when(documentRepository.findAll()).thenReturn(List.of(doc1, doc2));

            // Act
            List<Document> result = ragService.listDocuments();

            // Assert
            assertThat(result).hasSize(2);
            verify(documentRepository).findAll();
        }

        @Test
        @DisplayName("should return empty list when no documents exist")
        void shouldReturnEmptyListWhenNoDocumentsExist() {
            // Arrange
            when(documentRepository.findAll()).thenReturn(List.of());

            // Act
            List<Document> result = ragService.listDocuments();

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("deleteDocument")
    class DeleteDocument {

        @Test
        @DisplayName("should delete document from repository")
        void shouldDeleteDocumentFromRepository() {
            // Arrange
            UUID docId = UUID.randomUUID();
            Document doc = createTestDocument("To Delete", DocumentStatus.READY);
            when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));

            // Act
            ragService.deleteDocument(docId);

            // Assert
            verify(documentRepository).delete(docId);
        }

        @Test
        @DisplayName("should throw exception when document not found")
        void shouldThrowExceptionWhenDocumentNotFound() {
            // Arrange
            UUID docId = UUID.randomUUID();
            when(documentRepository.findById(docId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> ragService.deleteDocument(docId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Document not found");
        }

        @Test
        @DisplayName("should call vector search before deletion")
        void shouldCallVectorSearchBeforeDeletion() {
            // Arrange
            UUID docId = UUID.randomUUID();
            Document doc = createTestDocument("To Delete", DocumentStatus.READY);
            when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));
            when(embeddingAdapter.getDimensions()).thenReturn(EMBEDDING_DIMENSIONS);
            when(vectorAdapter.search(any(), anyInt(), any())).thenReturn(List.of());

            // Act
            ragService.deleteDocument(docId);

            // Assert
            verify(vectorAdapter).search(any(), anyInt(), eq(List.of(docId)));
        }
    }

    @Nested
    @DisplayName("retrieveContext")
    class RetrieveContext {

        @Test
        @DisplayName("should return context and sources for given query")
        void shouldReturnContextAndSourcesForGivenQuery() {
            // Arrange
            String query = "What is AI?";
            float[] queryEmbedding = new float[EMBEDDING_DIMENSIONS];
            when(embeddingAdapter.embed(query)).thenReturn(queryEmbedding);

            List<DocumentChunk> chunks = List.of(
                createTestChunk("AI is artificial intelligence."),
                createTestChunk("Machine learning is part of AI.")
            );
            when(vectorAdapter.search(eq(queryEmbedding), eq(5))).thenReturn(chunks);

            // Act
            RagService.RetrievalResult result = ragService.retrieveContext(query, null, 5);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.context()).contains("AI is artificial intelligence");
            assertThat(result.sources()).hasSize(2);
        }

        @Test
        @DisplayName("should use default topK when not specified")
        void shouldUseDefaultTopKWhenNotSpecified() {
            // Arrange
            String query = "Test query";
            when(embeddingAdapter.embed(query)).thenReturn(new float[EMBEDDING_DIMENSIONS]);
            when(vectorAdapter.search(any(), anyInt())).thenReturn(List.of());

            // Act
            RagService.RetrievalResult result = ragService.retrieveContext(query, null, 0);

            // Assert
            verify(vectorAdapter).search(any(), eq(5));
        }

        @Test
        @DisplayName("should filter by document IDs when provided")
        void shouldFilterByDocumentIdsWhenProvided() {
            // Arrange
            String query = "Test query";
            UUID docId1 = UUID.randomUUID();
            UUID docId2 = UUID.randomUUID();
            List<UUID> docIds = List.of(docId1, docId2);

            when(embeddingAdapter.embed(query)).thenReturn(new float[EMBEDDING_DIMENSIONS]);
            when(vectorAdapter.search(any(), eq(3), eq(docIds))).thenReturn(List.of());

            // Act
            ragService.retrieveContext(query, docIds, 3);

            // Assert
            verify(vectorAdapter).search(any(), eq(3), eq(docIds));
        }

        @Test
        @DisplayName("should calculate similarity scores for sources")
        void shouldCalculateSimilarityScoresForSources() {
            // Arrange
            String query = "Test query";
            float[] queryEmbedding = {0.1f, 0.2f, 0.3f};
            when(embeddingAdapter.embed(query)).thenReturn(queryEmbedding);

            DocumentChunk chunk = createTestChunk("Relevant content here.");
            chunk = chunk.withEmbedding(new float[]{0.1f, 0.2f, 0.3f});
            when(vectorAdapter.search(any(), anyInt())).thenReturn(List.of(chunk));

            // Act
            RagService.RetrievalResult result = ragService.retrieveContext(query, null, 5);

            // Assert
            assertThat(result.sources()).hasSize(1);
            assertThat(result.sources().get(0).score()).isGreaterThan(0.0);
        }

        @Test
        @DisplayName("should enrich query with context")
        void shouldEnrichQueryWithContext() {
            // Arrange
            String query = "Original query";
            when(embeddingAdapter.embed(query)).thenReturn(new float[EMBEDDING_DIMENSIONS]);
            when(vectorAdapter.search(any(), anyInt())).thenReturn(List.of());

            // Act
            RagService.RetrievalResult result = ragService.retrieveContext(query, null, 5);

            // Assert
            assertThat(result.enrichedQuery()).isEqualTo(query);
        }
    }

    private Document createTestDocument(String title, DocumentStatus status) {
        Document doc = new Document(
            DocumentId.generate(),
            title,
            title + ".txt",
            1024L,
            status,
            java.time.Instant.now(),
            java.time.Instant.now()
        );
        return doc;
    }

    private DocumentChunk createTestChunk(String content) {
        return new DocumentChunk(
            UUID.randomUUID(),
            UUID.randomUUID(),
            content,
            0,
            Map.of("source", "test")
        ).withEmbedding(new float[EMBEDDING_DIMENSIONS]);
    }
}
