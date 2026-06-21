package com.ai.modules.rag.application.usecase;

import com.ai.modules.rag.application.usecase.RetrievalService;
import com.ai.modules.rag.domain.model.SourceDocument;
import com.ai.modules.rag.infrastructure.llm.EmbeddingAdapter;
import com.ai.modules.rag.infrastructure.vector.PgVectorAdapter;
import com.ai.modules.rag.domain.model.DocumentChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RetrievalService Unit Tests
 *
 * Tests for RAG retrieval operations following TDD principles:
 * - Naming convention: should_expected_result_when_condition
 * - Uses AAA pattern (Arrange-Act-Assert)
 * - Pure unit tests with mocked dependencies
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RetrievalService")
class RetrievalServiceTest {

    @Mock
    private EmbeddingAdapter embeddingAdapter;

    @Mock
    private PgVectorAdapter vectorAdapter;

    private RetrievalService retrievalService;

    private static final int EMBEDDING_DIMENSIONS = 384;

    @BeforeEach
    void setUp() {
        retrievalService = new RetrievalService(embeddingAdapter, vectorAdapter);
    }

    // ============ retrieve() Tests ============

    @Nested
    @DisplayName("retrieve")
    class Retrieve {

        @Test
        @DisplayName("should return retrieval result with context and sources")
        void shouldReturnRetrievalResultWithContextAndSources() {
            // Arrange
            String query = "What is AI?";
            float[] mockEmbedding = createMockEmbedding();
            float[] chunkEmbedding = createMockEmbedding();
            chunkEmbedding[0] = 0.9f;

            DocumentChunk chunk = DocumentChunk.create(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "AI is Artificial Intelligence.",
                    0,
                    Map.of("title", "Test Doc")
            ).withEmbedding(chunkEmbedding);

            when(embeddingAdapter.embed(query)).thenReturn(mockEmbedding);
            when(vectorAdapter.search(any(), anyInt())).thenReturn(List.of(chunk));

            // Act
            RetrievalService.RetrievalResult result = retrievalService.retrieve(query, null, 5);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.context()).contains("AI is Artificial Intelligence");
            assertThat(result.sources()).hasSize(1);
            assertThat(result.enrichedQuery()).isEqualTo(query);
        }

        @Test
        @DisplayName("should return empty result when no chunks found")
        void shouldReturnEmptyResultWhenNoChunksFound() {
            // Arrange
            String query = "No results query";
            float[] mockEmbedding = createMockEmbedding();

            when(embeddingAdapter.embed(query)).thenReturn(mockEmbedding);
            when(vectorAdapter.search(any(), anyInt())).thenReturn(List.of());

            // Act
            RetrievalService.RetrievalResult result = retrievalService.retrieve(query, null, 5);

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
            float[] mockEmbedding = createMockEmbedding();

            when(embeddingAdapter.embed(query)).thenReturn(mockEmbedding);
            when(vectorAdapter.search(any(), anyInt(), any())).thenReturn(List.of());

            // Act
            RetrievalService.RetrievalResult result = retrievalService.retrieve(query, docIds, 5);

            // Assert
            assertThat(result).isNotNull();
            verify(vectorAdapter).search(any(), eq(5), eq(docIds));
        }

        @Test
        @DisplayName("should use default topK when not provided")
        void shouldUseDefaultTopKWhenNotProvided() {
            // Arrange
            String query = "Test query";
            float[] mockEmbedding = createMockEmbedding();

            when(embeddingAdapter.embed(query)).thenReturn(mockEmbedding);
            when(vectorAdapter.search(any(), eq(5))).thenReturn(List.of());

            // Act
            retrievalService.retrieve(query, null, 0);

            // Assert
            verify(vectorAdapter).search(any(), eq(5));
        }

        @Test
        @DisplayName("should use provided topK when positive")
        void shouldUseProvidedTopKWhenPositive() {
            // Arrange
            String query = "Test query";
            float[] mockEmbedding = createMockEmbedding();

            when(embeddingAdapter.embed(query)).thenReturn(mockEmbedding);
            when(vectorAdapter.search(any(), eq(10))).thenReturn(List.of());

            // Act
            retrievalService.retrieve(query, null, 10);

            // Assert
            verify(vectorAdapter).search(any(), eq(10));
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
            RetrievalService.RetrievalResult result = new RetrievalService.RetrievalResult(
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
            RetrievalService.RetrievalResult result1 = new RetrievalService.RetrievalResult(
                    "ctx", List.of(), "q"
            );
            RetrievalService.RetrievalResult result2 = new RetrievalService.RetrievalResult(
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
            RetrievalService.RetrievalResult result1 = new RetrievalService.RetrievalResult(
                    "ctx1", List.of(), "q"
            );
            RetrievalService.RetrievalResult result2 = new RetrievalService.RetrievalResult(
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

    // ============ calculateSimilarity edge cases (via private method testing) ============

    @Nested
    @DisplayName("calculateSimilarity edge cases")
    class CalculateSimilarityEdgeCases {

        @Test
        @DisplayName("should return zero when query embedding is null")
        void shouldReturnZeroWhenQueryEmbeddingIsNull() {
            // Arrange
            String query = "Test query";
            float[] nullQueryEmbedding = null;
            float[] validChunkEmbedding = createMockEmbedding();

            DocumentChunk chunk = DocumentChunk.create(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "content",
                    0,
                    Map.of()
            ).withEmbedding(validChunkEmbedding);

            when(embeddingAdapter.embed(query)).thenReturn(nullQueryEmbedding);
            when(vectorAdapter.search(any(), anyInt())).thenReturn(List.of(chunk));

            // Act
            RetrievalService.RetrievalResult result = retrievalService.retrieve(query, null, 5);

            // Assert - should handle gracefully
            assertThat(result.sources()).hasSize(1);
            assertThat(result.sources().get(0).score()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should return zero when chunk embedding is null")
        void shouldReturnZeroWhenChunkEmbeddingIsNull() {
            // Arrange
            String query = "Test query";
            float[] validQueryEmbedding = createMockEmbedding();

            DocumentChunk chunk = DocumentChunk.create(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "content",
                    0,
                    Map.of()
            ); // No embedding set

            when(embeddingAdapter.embed(query)).thenReturn(validQueryEmbedding);
            when(vectorAdapter.search(any(), anyInt())).thenReturn(List.of(chunk));

            // Act
            RetrievalService.RetrievalResult result = retrievalService.retrieve(query, null, 5);

            // Assert - should handle gracefully
            assertThat(result.sources()).hasSize(1);
            assertThat(result.sources().get(0).score()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should return zero when both embeddings are null")
        void shouldReturnZeroWhenBothEmbeddingsAreNull() {
            // Arrange
            String query = "Test query";
            float[] nullEmbedding = null;

            DocumentChunk chunk = DocumentChunk.create(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "content",
                    0,
                    Map.of()
            );

            when(embeddingAdapter.embed(query)).thenReturn(nullEmbedding);
            when(vectorAdapter.search(any(), anyInt())).thenReturn(List.of(chunk));

            // Act
            RetrievalService.RetrievalResult result = retrievalService.retrieve(query, null, 5);

            // Assert
            assertThat(result.sources()).hasSize(1);
            assertThat(result.sources().get(0).score()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should return zero when embeddings have different lengths")
        void shouldReturnZeroWhenEmbeddingsHaveDifferentLengths() {
            // Arrange
            String query = "Test query";
            float[] queryEmbedding = new float[384]; // Standard size
            Arrays.fill(queryEmbedding, 0.1f);

            float[] chunkEmbedding = new float[256]; // Different size

            DocumentChunk chunk = DocumentChunk.create(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "content",
                    0,
                    Map.of()
            ).withEmbedding(chunkEmbedding);

            when(embeddingAdapter.embed(query)).thenReturn(queryEmbedding);
            when(vectorAdapter.search(any(), anyInt())).thenReturn(List.of(chunk));

            // Act
            RetrievalService.RetrievalResult result = retrievalService.retrieve(query, null, 5);

            // Assert
            assertThat(result.sources()).hasSize(1);
            assertThat(result.sources().get(0).score()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should return zero when embeddings have zero magnitude")
        void shouldReturnZeroWhenEmbeddingsHaveZeroMagnitude() {
            // Arrange
            String query = "Test query";
            float[] zeroQueryEmbedding = new float[384]; // All zeros
            float[] zeroChunkEmbedding = new float[384]; // All zeros

            DocumentChunk chunk = DocumentChunk.create(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "content",
                    0,
                    Map.of()
            ).withEmbedding(zeroChunkEmbedding);

            when(embeddingAdapter.embed(query)).thenReturn(zeroQueryEmbedding);
            when(vectorAdapter.search(any(), anyInt())).thenReturn(List.of(chunk));

            // Act
            RetrievalService.RetrievalResult result = retrievalService.retrieve(query, null, 5);

            // Assert
            assertThat(result.sources()).hasSize(1);
            assertThat(result.sources().get(0).score()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should calculate high similarity for identical embeddings")
        void shouldCalculateHighSimilarityForIdenticalEmbeddings() {
            // Arrange
            String query = "Test query";
            float[] identicalEmbedding = createMockEmbedding();

            DocumentChunk chunk = DocumentChunk.create(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "content",
                    0,
                    Map.of()
            ).withEmbedding(identicalEmbedding);

            when(embeddingAdapter.embed(query)).thenReturn(identicalEmbedding);
            when(vectorAdapter.search(any(), anyInt())).thenReturn(List.of(chunk));

            // Act
            RetrievalService.RetrievalResult result = retrievalService.retrieve(query, null, 5);

            // Assert - identical vectors should have similarity close to 1.0
            assertThat(result.sources()).hasSize(1);
            assertThat(result.sources().get(0).score()).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.001));
        }

        @Test
        @DisplayName("should calculate negative similarity for opposite embeddings")
        void shouldCalculateNegativeSimilarityForOppositeEmbeddings() {
            // Arrange
            String query = "Test query";
            float[] queryEmbedding = createMockEmbedding();
            float[] oppositeChunkEmbedding = createMockEmbedding();
            for (int i = 0; i < oppositeChunkEmbedding.length; i++) {
                oppositeChunkEmbedding[i] = -queryEmbedding[i]; // Opposite direction
            }

            DocumentChunk chunk = DocumentChunk.create(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "content",
                    0,
                    Map.of()
            ).withEmbedding(oppositeChunkEmbedding);

            when(embeddingAdapter.embed(query)).thenReturn(queryEmbedding);
            when(vectorAdapter.search(any(), anyInt())).thenReturn(List.of(chunk));

            // Act
            RetrievalService.RetrievalResult result = retrievalService.retrieve(query, null, 5);

            // Assert - opposite vectors should have similarity close to -1.0
            assertThat(result.sources()).hasSize(1);
            assertThat(result.sources().get(0).score()).isCloseTo(-1.0, org.assertj.core.data.Offset.offset(0.001));
        }
    }

    // ============ Edge Cases for Coverage ============

    @Nested
    @DisplayName("retrieve with edge cases")
    class RetrieveEdgeCases {

        @Test
        @DisplayName("should truncate long content in sources")
        void shouldTruncateLongContentInSources() {
            // Arrange
            String query = "Test query";
            float[] mockEmbedding = createMockEmbedding();
            String longContent = "A".repeat(600); // > MAX_SOURCE_LENGTH (500)

            DocumentChunk chunk = DocumentChunk.create(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    longContent,
                    0,
                    Map.of()
            ).withEmbedding(createMockEmbedding());

            when(embeddingAdapter.embed(query)).thenReturn(mockEmbedding);
            when(vectorAdapter.search(any(), anyInt())).thenReturn(List.of(chunk));

            // Act
            RetrievalService.RetrievalResult result = retrievalService.retrieve(query, null, 5);

            // Assert
            assertThat(result.sources()).hasSize(1);
            assertThat(result.sources().get(0).text()).hasSize(500);
        }

        @Test
        @DisplayName("should use short content as-is without truncation")
        void shouldUseShortContentAsIsWithoutTruncation() {
            // Arrange
            String query = "Test query";
            float[] mockEmbedding = createMockEmbedding();
            String shortContent = "Short content";

            DocumentChunk chunk = DocumentChunk.create(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    shortContent,
                    0,
                    Map.of()
            ).withEmbedding(createMockEmbedding());

            when(embeddingAdapter.embed(query)).thenReturn(mockEmbedding);
            when(vectorAdapter.search(any(), anyInt())).thenReturn(List.of(chunk));

            // Act
            RetrievalService.RetrievalResult result = retrievalService.retrieve(query, null, 5);

            // Assert
            assertThat(result.sources()).hasSize(1);
            assertThat(result.sources().get(0).text()).isEqualTo(shortContent);
        }

        @Test
        @DisplayName("should truncate content exactly at boundary")
        void shouldTruncateContentExactlyAtBoundary() {
            // Arrange
            String query = "Test query";
            float[] mockEmbedding = createMockEmbedding();
            String boundaryContent = "A".repeat(500); // Exactly at MAX_SOURCE_LENGTH

            DocumentChunk chunk = DocumentChunk.create(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    boundaryContent,
                    0,
                    Map.of()
            ).withEmbedding(createMockEmbedding());

            when(embeddingAdapter.embed(query)).thenReturn(mockEmbedding);
            when(vectorAdapter.search(any(), anyInt())).thenReturn(List.of(chunk));

            // Act
            RetrievalService.RetrievalResult result = retrievalService.retrieve(query, null, 5);

            // Assert
            assertThat(result.sources()).hasSize(1);
            assertThat(result.sources().get(0).text()).hasSize(500);
        }

        @Test
        @DisplayName("should sort sources by score descending")
        void shouldSortSourcesByScoreDescending() {
            // Arrange
            String query = "Test query";
            float[] mockEmbedding = createMockEmbedding();
            mockEmbedding[0] = 0.5f;

            float[] highScoreEmbedding = createMockEmbedding();
            highScoreEmbedding[0] = 0.9f;

            float[] lowScoreEmbedding = createMockEmbedding();
            lowScoreEmbedding[0] = 0.3f;

            DocumentChunk chunk1 = DocumentChunk.create(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "Low score chunk",
                    0,
                    Map.of()
            ).withEmbedding(lowScoreEmbedding);

            DocumentChunk chunk2 = DocumentChunk.create(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "High score chunk",
                    0,
                    Map.of()
            ).withEmbedding(highScoreEmbedding);

            when(embeddingAdapter.embed(query)).thenReturn(mockEmbedding);
            when(vectorAdapter.search(any(), anyInt())).thenReturn(List.of(chunk1, chunk2));

            // Act
            RetrievalService.RetrievalResult result = retrievalService.retrieve(query, null, 5);

            // Assert - sources should be sorted by score descending
            assertThat(result.sources()).hasSize(2);
            assertThat(result.sources().get(0).score()).isGreaterThan(result.sources().get(1).score());
        }

        @Test
        @DisplayName("should join multiple chunks with double newline")
        void shouldJoinMultipleChunksWithDoubleNewline() {
            // Arrange
            String query = "Test query";
            float[] mockEmbedding = createMockEmbedding();

            DocumentChunk chunk1 = DocumentChunk.create(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "First chunk",
                    0,
                    Map.of()
            ).withEmbedding(createMockEmbedding());

            DocumentChunk chunk2 = DocumentChunk.create(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "Second chunk",
                    0,
                    Map.of()
            ).withEmbedding(createMockEmbedding());

            when(embeddingAdapter.embed(query)).thenReturn(mockEmbedding);
            when(vectorAdapter.search(any(), anyInt())).thenReturn(List.of(chunk1, chunk2));

            // Act
            RetrievalService.RetrievalResult result = retrievalService.retrieve(query, null, 5);

            // Assert
            assertThat(result.context()).contains("First chunk");
            assertThat(result.context()).contains("Second chunk");
            assertThat(result.context()).contains("\n\n");
        }

        @Test
        @DisplayName("should handle single chunk in context")
        void shouldHandleSingleChunkInContext() {
            // Arrange
            String query = "Test query";
            float[] mockEmbedding = createMockEmbedding();
            String content = "Single chunk content";

            DocumentChunk chunk = DocumentChunk.create(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    content,
                    0,
                    Map.of()
            ).withEmbedding(createMockEmbedding());

            when(embeddingAdapter.embed(query)).thenReturn(mockEmbedding);
            when(vectorAdapter.search(any(), anyInt())).thenReturn(List.of(chunk));

            // Act
            RetrievalService.RetrievalResult result = retrievalService.retrieve(query, null, 5);

            // Assert
            assertThat(result.context()).isEqualTo(content);
            assertThat(result.sources()).hasSize(1);
        }

        @Test
        @DisplayName("should use default topK when topK is negative")
        void shouldUseDefaultTopKWhenTopKIsNegative() {
            // Arrange
            String query = "Test query";
            float[] mockEmbedding = createMockEmbedding();

            when(embeddingAdapter.embed(query)).thenReturn(mockEmbedding);
            when(vectorAdapter.search(any(), eq(5))).thenReturn(List.of());

            // Act
            retrievalService.retrieve(query, null, -1);

            // Assert
            verify(vectorAdapter).search(any(), eq(5));
        }

        @Test
        @DisplayName("should use default topK when topK is zero")
        void shouldUseDefaultTopKWhenTopKIsZero() {
            // Arrange
            String query = "Test query";
            float[] mockEmbedding = createMockEmbedding();

            when(embeddingAdapter.embed(query)).thenReturn(mockEmbedding);
            when(vectorAdapter.search(any(), eq(5))).thenReturn(List.of());

            // Act
            retrievalService.retrieve(query, null, 0);

            // Assert
            verify(vectorAdapter).search(any(), eq(5));
        }
    }
}
