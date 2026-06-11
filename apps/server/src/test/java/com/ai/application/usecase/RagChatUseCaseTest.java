package com.ai.application.usecase;

import com.ai.application.port.EmbeddingPort;
import com.ai.application.port.VectorSearchPort;
import com.ai.domain.model.DocumentChunk;
import com.ai.domain.model.SourceDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * RagChatUseCase Unit Tests
 * 
 * Tests using Mockito to mock external dependencies (ports):
 * - Naming convention: should_expected_result_when_condition
 * - Uses AAA pattern (Arrange-Act-Assert)
 * - Tests RAG retrieval flow
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RagChatUseCase")
class RagChatUseCaseTest {

    @Mock
    private EmbeddingPort embeddingPort;

    @Mock
    private VectorSearchPort vectorSearchPort;

    private RagChatUseCase useCase;

    private static final String TEST_QUERY = "What is AI?";
    private static final float[] TEST_EMBEDDING = new float[]{0.1f, 0.2f, 0.3f};
    private static final int DEFAULT_TOP_K = 5;

    @BeforeEach
    void setUp() {
        useCase = new RagChatUseCase(embeddingPort, vectorSearchPort);
    }

    @Nested
    @DisplayName("execute")
    class Execute {

        @Test
        @DisplayName("should embed question before searching vectors")
        void shouldEmbedQuestionBeforeSearchingVectors() {
            // Arrange
            when(embeddingPort.embed(TEST_QUERY)).thenReturn(TEST_EMBEDDING);
            when(vectorSearchPort.search(any(float[].class), anyInt())).thenReturn(Collections.emptyList());

            // Act
            useCase.execute(TEST_QUERY, null, DEFAULT_TOP_K);

            // Assert
            verify(embeddingPort).embed(TEST_QUERY);
            verify(vectorSearchPort).search(eq(TEST_EMBEDDING), eq(DEFAULT_TOP_K));
        }

        @Test
        @DisplayName("should search vectors with query embedding")
        void shouldSearchVectorsWithQueryEmbedding() {
            // Arrange
            when(embeddingPort.embed(TEST_QUERY)).thenReturn(TEST_EMBEDDING);
            when(vectorSearchPort.search(any(float[].class), anyInt())).thenReturn(Collections.emptyList());

            // Act
            useCase.execute(TEST_QUERY, null, DEFAULT_TOP_K);

            // Assert
            verify(vectorSearchPort).search(any(float[].class), eq(DEFAULT_TOP_K));
        }

        @Test
        @DisplayName("should construct context from chunks")
        void shouldConstructContextFromChunks() {
            // Arrange
            UUID docId = UUID.randomUUID();
            float[] queryEmbedding = new float[]{0.1f, 0.1f}; // Same size as chunk embeddings
            when(embeddingPort.embed(TEST_QUERY)).thenReturn(queryEmbedding);
            
            List<DocumentChunk> chunks = List.of(
                createChunkWithEmbedding(UUID.randomUUID(), docId, "First chunk content", 2),
                createChunkWithEmbedding(UUID.randomUUID(), docId, "Second chunk content", 2)
            );
            when(vectorSearchPort.search(any(float[].class), anyInt())).thenReturn(chunks);

            // Act
            RagChatUseCase.RetrievalResult result = useCase.execute(TEST_QUERY, null, DEFAULT_TOP_K);

            // Assert
            assertThat(result.context()).contains("First chunk content");
            assertThat(result.context()).contains("Second chunk content");
        }

        @Test
        @DisplayName("should return empty context when no vectors found")
        void shouldReturnEmptyContextWhenNoVectorsFound() {
            // Arrange
            when(embeddingPort.embed(TEST_QUERY)).thenReturn(TEST_EMBEDDING);
            when(vectorSearchPort.search(any(float[].class), anyInt())).thenReturn(Collections.emptyList());

            // Act
            RagChatUseCase.RetrievalResult result = useCase.execute(TEST_QUERY, null, DEFAULT_TOP_K);

            // Assert
            assertThat(result.context()).isEmpty();
            assertThat(result.sources()).isEmpty();
        }

        @Test
        @DisplayName("should limit to top-k results")
        void shouldLimitToTopKResults() {
            // Arrange
            UUID docId = UUID.randomUUID();
            when(embeddingPort.embed(TEST_QUERY)).thenReturn(new float[]{0.1f, 0.1f});
            
            List<DocumentChunk> manyChunks = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                manyChunks.add(createChunkWithEmbedding(UUID.randomUUID(), docId, "Content " + i, 2));
            }
            when(vectorSearchPort.search(any(float[].class), eq(3))).thenReturn(manyChunks.subList(0, 3));

            // Act
            RagChatUseCase.RetrievalResult result = useCase.execute(TEST_QUERY, null, 3);

            // Assert
            verify(vectorSearchPort).search(any(float[].class), eq(3));
        }

        @Test
        @DisplayName("should use default top-k when zero provided")
        void shouldUseDefaultTopKWhenZeroProvided() {
            // Arrange
            when(embeddingPort.embed(TEST_QUERY)).thenReturn(TEST_EMBEDDING);
            when(vectorSearchPort.search(any(float[].class), eq(DEFAULT_TOP_K))).thenReturn(Collections.emptyList());

            // Act
            useCase.execute(TEST_QUERY, null, 0);

            // Assert
            verify(vectorSearchPort).search(any(float[].class), eq(DEFAULT_TOP_K));
        }

        @Test
        @DisplayName("should use default top-k when negative provided")
        void shouldUseDefaultTopKWhenNegativeProvided() {
            // Arrange
            when(embeddingPort.embed(TEST_QUERY)).thenReturn(TEST_EMBEDDING);
            when(vectorSearchPort.search(any(float[].class), eq(DEFAULT_TOP_K))).thenReturn(Collections.emptyList());

            // Act
            useCase.execute(TEST_QUERY, null, -5);

            // Assert
            verify(vectorSearchPort).search(any(float[].class), eq(DEFAULT_TOP_K));
        }
    }

    @Nested
    @DisplayName("with Document IDs")
    class WithDocumentIds {

        @Test
        @DisplayName("should filter search by document IDs when provided")
        void shouldFilterSearchByDocumentIdsWhenProvided() {
            // Arrange
            UUID docId1 = UUID.randomUUID();
            UUID docId2 = UUID.randomUUID();
            List<UUID> docIds = List.of(docId1, docId2);
            
            when(embeddingPort.embed(TEST_QUERY)).thenReturn(TEST_EMBEDDING);
            when(vectorSearchPort.search(any(float[].class), anyInt(), any())).thenReturn(Collections.emptyList());

            // Act
            useCase.execute(TEST_QUERY, docIds, DEFAULT_TOP_K);

            // Assert
            verify(vectorSearchPort).search(eq(TEST_EMBEDDING), eq(DEFAULT_TOP_K), eq(docIds));
        }

        @Test
        @DisplayName("should search without doc IDs when list is empty")
        void shouldSearchWithoutDocIdsWhenListIsEmpty() {
            // Arrange
            when(embeddingPort.embed(TEST_QUERY)).thenReturn(TEST_EMBEDDING);
            when(vectorSearchPort.search(any(float[].class), anyInt())).thenReturn(Collections.emptyList());

            // Act
            useCase.execute(TEST_QUERY, Collections.emptyList(), DEFAULT_TOP_K);

            // Assert
            verify(vectorSearchPort).search(any(float[].class), eq(DEFAULT_TOP_K));
            verify(vectorSearchPort, never()).search(any(float[].class), anyInt(), any());
        }

        @Test
        @DisplayName("should search without doc IDs when null")
        void shouldSearchWithoutDocIdsWhenNull() {
            // Arrange
            when(embeddingPort.embed(TEST_QUERY)).thenReturn(TEST_EMBEDDING);
            when(vectorSearchPort.search(any(float[].class), anyInt())).thenReturn(Collections.emptyList());

            // Act
            useCase.execute(TEST_QUERY, null, DEFAULT_TOP_K);

            // Assert
            verify(vectorSearchPort).search(any(float[].class), eq(DEFAULT_TOP_K));
            verify(vectorSearchPort, never()).search(any(float[].class), anyInt(), any());
        }
    }

    @Nested
    @DisplayName("Source Documents")
    class SourceDocuments {

        @Test
        @DisplayName("should create source documents with scores")
        void shouldCreateSourceDocumentsWithScores() {
            // Arrange
            UUID docId = UUID.randomUUID();
            when(embeddingPort.embed(TEST_QUERY)).thenReturn(new float[]{0.5f, 0.5f});
            
            List<DocumentChunk> chunks = List.of(
                createChunkWithEmbedding(UUID.randomUUID(), docId, "Content with relevant info", 2)
            );
            when(vectorSearchPort.search(any(float[].class), anyInt())).thenReturn(chunks);

            // Act
            RagChatUseCase.RetrievalResult result = useCase.execute(TEST_QUERY, null, DEFAULT_TOP_K);

            // Assert
            assertThat(result.sources()).hasSize(1);
            assertThat(result.sources().get(0).text()).contains("Content with relevant info");
            assertThat(result.sources().get(0).score()).isGreaterThanOrEqualTo(0.0);
        }

        @Test
        @DisplayName("should truncate long content in sources")
        void shouldTruncateLongContentInSources() {
            // Arrange
            UUID docId = UUID.randomUUID();
            String longContent = "A".repeat(600);
            when(embeddingPort.embed(TEST_QUERY)).thenReturn(new float[]{0.5f, 0.5f});
            
            List<DocumentChunk> chunks = List.of(
                createChunkWithEmbedding(UUID.randomUUID(), docId, longContent, 2)
            );
            when(vectorSearchPort.search(any(float[].class), anyInt())).thenReturn(chunks);

            // Act
            RagChatUseCase.RetrievalResult result = useCase.execute(TEST_QUERY, null, DEFAULT_TOP_K);

            // Assert
            assertThat(result.sources().get(0).text()).hasSize(500);
        }

        @Test
        @DisplayName("should include metadata in sources")
        void shouldIncludeMetadataInSources() {
            // Arrange
            UUID docId = UUID.randomUUID();
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", "test.pdf");
            metadata.put("page", 1);
            
            DocumentChunk chunk = createChunkWithMetadata(UUID.randomUUID(), docId, "Test content", metadata, 2);
            
            when(embeddingPort.embed(TEST_QUERY)).thenReturn(new float[]{0.5f, 0.5f});
            when(vectorSearchPort.search(any(float[].class), anyInt())).thenReturn(List.of(chunk));

            // Act
            RagChatUseCase.RetrievalResult result = useCase.execute(TEST_QUERY, null, DEFAULT_TOP_K);

            // Assert
            assertThat(result.sources().get(0).metadata()).containsEntry("source", "test.pdf");
            assertThat(result.sources().get(0).metadata()).containsEntry("page", 1);
        }
    }

    @Nested
    @DisplayName("Context Construction")
    class ContextConstruction {

        @Test
        @DisplayName("should join chunks with double newline separator")
        void shouldJoinChunksWithDoubleNewlineSeparator() {
            // Arrange
            UUID docId = UUID.randomUUID();
            when(embeddingPort.embed(TEST_QUERY)).thenReturn(new float[]{0.5f, 0.5f});
            
            List<DocumentChunk> chunks = List.of(
                createChunkWithEmbedding(UUID.randomUUID(), docId, "First", 2),
                createChunkWithEmbedding(UUID.randomUUID(), docId, "Second", 2),
                createChunkWithEmbedding(UUID.randomUUID(), docId, "Third", 2)
            );
            when(vectorSearchPort.search(any(float[].class), anyInt())).thenReturn(chunks);

            // Act
            RagChatUseCase.RetrievalResult result = useCase.execute(TEST_QUERY, null, DEFAULT_TOP_K);

            // Assert
            assertThat(result.context()).contains("First");
            assertThat(result.context()).contains("Second");
            assertThat(result.context()).contains("Third");
        }
    }

    /**
     * Helper method to create DocumentChunk with embedding set via withEmbedding.
     * The chunk content is used to calculate similarity, so we use meaningful content.
     */
    private DocumentChunk createChunkWithEmbedding(UUID chunkId, UUID docId, String content, int embeddingSize) {
        // Use a simple embedding that gives predictable similarity
        float[] embedding = new float[embeddingSize];
        Arrays.fill(embedding, 0.1f);
        DocumentChunk chunk = new DocumentChunk(
            chunkId, docId, content, 0, new HashMap<>()
        );
        return chunk.withEmbedding(embedding);
    }

    /**
     * Helper method to create DocumentChunk with specific metadata.
     */
    private DocumentChunk createChunkWithMetadata(UUID chunkId, UUID docId, String content, Map<String, Object> metadata, int embeddingSize) {
        float[] embedding = new float[embeddingSize];
        Arrays.fill(embedding, 0.1f);
        DocumentChunk chunk = new DocumentChunk(
            chunkId, docId, content, 0, metadata
        );
        return chunk.withEmbedding(embedding);
    }
}
