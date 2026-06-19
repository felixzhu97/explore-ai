package com.ai.domain.service;

import com.ai.adapter.out.embedding.EmbeddingAdapter;
import com.ai.adapter.out.vector.PgVectorAdapter;
import com.ai.domain.model.DocumentChunk;
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

            DocumentChunk chunk = new DocumentChunk(
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
                    List.of(new com.ai.domain.model.SourceDocument("source", 0.9, Map.of())),
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
}
