package com.ai.rag.application.usecase;

import com.ai.rag.domain.model.DocumentChunk;
import com.ai.rag.domain.model.SourceDocument;
import com.ai.rag.domain.util.VectorSimilarity;
import com.ai.rag.domain.vo.DocumentId;
import com.ai.rag.domain.repository.DocumentChunkSearchRepository;
import com.ai.rag.domain.repository.RagRetrievalSettings;
import com.ai.rag.domain.repository.TextEmbeddingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentSearchService")
class DocumentSearchServiceTest {

    @Mock
    private TextEmbeddingRepository embeddingRepository;

    @Mock
    private DocumentChunkSearchRepository chunkSearchRepository;

    @Mock
    private RagRetrievalSettings retrievalSettings;

    private DocumentSearchService service;

    @BeforeEach
    void setUp() {
        lenient().when(retrievalSettings.getTopK()).thenReturn(5);
        lenient().when(retrievalSettings.getScoreThreshold()).thenReturn(0.0);

        service = new DocumentSearchService(embeddingRepository, chunkSearchRepository, retrievalSettings);
    }

    @Nested
    @DisplayName("retrieve()")
    class Retrieve {

        @Test
        @DisplayName("should retrieve documents for query without docIds filter")
        void shouldRetrieveDocumentsForQueryWithoutDocIdsFilter() {
            String query = "What is AI?";
            float[] queryEmbedding = new float[]{0.1f, 0.2f, 0.3f};
            List<DocumentChunk> chunks = List.of(
                    createChunk("AI stands for Artificial Intelligence", 0),
                    createChunk("Machine learning is a subset of AI", 1)
            );

            when(embeddingRepository.embed(query)).thenReturn(queryEmbedding);
            when(chunkSearchRepository.search(queryEmbedding, 5)).thenReturn(chunks);

            DocumentSearchService.RetrievalResult result = service.retrieve(query, null, 5);

            assertThat(result.context()).contains("AI stands for Artificial Intelligence");
            assertThat(result.context()).contains("Machine learning is a subset of AI");
            assertThat(result.sources()).hasSize(2);
            verify(chunkSearchRepository).search(queryEmbedding, 5);
        }

        @Test
        @DisplayName("should filter by document IDs when provided")
        void shouldFilterByDocumentIdsWhenProvided() {
            String query = "test query";
            float[] queryEmbedding = new float[]{0.1f, 0.2f};
            DocumentId docId = DocumentId.generate();
            List<DocumentChunk> chunks = List.of(createChunk("filtered content", 0));

            when(embeddingRepository.embed(query)).thenReturn(queryEmbedding);
            when(chunkSearchRepository.search(queryEmbedding, 5, List.of(docId.value()))).thenReturn(chunks);

            DocumentSearchService.RetrievalResult result = service.retrieve(query, List.of(docId), 5);

            assertThat(result.sources()).hasSize(1);
            verify(chunkSearchRepository).search(queryEmbedding, 5, List.of(docId.value()));
        }

        @Test
        @DisplayName("should use default topK when not specified")
        void shouldUseDefaultTopKWhenNotSpecified() {
            String query = "test";
            float[] queryEmbedding = new float[]{0.1f, 0.2f};

            when(embeddingRepository.embed(query)).thenReturn(queryEmbedding);
            when(chunkSearchRepository.search(queryEmbedding, 5)).thenReturn(List.of());

            service.retrieve(query, null, 0);

            verify(chunkSearchRepository).search(queryEmbedding, 5);
        }

        @Test
        @DisplayName("should use provided topK when positive")
        void shouldUseProvidedTopKWhenPositive() {
            String query = "test";
            float[] queryEmbedding = new float[]{0.1f, 0.2f};

            when(embeddingRepository.embed(query)).thenReturn(queryEmbedding);
            when(chunkSearchRepository.search(queryEmbedding, 10)).thenReturn(List.of());

            service.retrieve(query, null, 10);

            verify(chunkSearchRepository).search(queryEmbedding, 10);
        }

        @Test
        @DisplayName("should sort sources by similarity score descending")
        void shouldSortSourcesBySimilarityScoreDescending() {
            String query = "test";
            float[] queryEmbedding = new float[]{0.5f, 0.5f};
            float[] highSimEmbedding = new float[]{0.95f, 0.05f};
            float[] medSimEmbedding = new float[]{0.75f, 0.25f};
            float[] lowSimEmbedding = new float[]{0.5f, 0.5f};

            DocumentChunk highSimChunk = DocumentChunk.reconstitute(
                    DocumentId.generate(), DocumentId.generate(), "high similarity content", 0,
                    Map.of(), highSimEmbedding, Instant.now());
            DocumentChunk medSimChunk = DocumentChunk.reconstitute(
                    DocumentId.generate(), DocumentId.generate(), "medium similarity content", 1,
                    Map.of(), medSimEmbedding, Instant.now());
            DocumentChunk lowSimChunk = DocumentChunk.reconstitute(
                    DocumentId.generate(), DocumentId.generate(), "low similarity content", 2,
                    Map.of(), lowSimEmbedding, Instant.now());

            when(embeddingRepository.embed(query)).thenReturn(queryEmbedding);
            when(chunkSearchRepository.search(any(), anyInt())).thenReturn(
                    List.of(lowSimChunk, highSimChunk, medSimChunk));

            DocumentSearchService.RetrievalResult result = service.retrieve(query, null, 5);

            assertThat(result.sources()).hasSize(3);
            assertThat(result.sources().get(0).score()).isGreaterThan(result.sources().get(1).score());
            assertThat(result.sources().get(1).score()).isGreaterThan(result.sources().get(2).score());
        }

        @Test
        @DisplayName("should return empty result when no chunks found")
        void shouldReturnEmptyResultWhenNoChunksFound() {
            String query = "nonexistent topic";
            float[] queryEmbedding = new float[]{0.1f, 0.2f};

            when(embeddingRepository.embed(query)).thenReturn(queryEmbedding);
            when(chunkSearchRepository.search(queryEmbedding, 5)).thenReturn(List.of());

            DocumentSearchService.RetrievalResult result = service.retrieve(query, null, 5);

            assertThat(result.context()).isEmpty();
            assertThat(result.sources()).isEmpty();
        }

        @Test
        @DisplayName("should join chunks with double newline separator")
        void shouldJoinChunksWithDoubleNewlineSeparator() {
            String query = "test";
            float[] queryEmbedding = new float[]{0.1f, 0.2f};
            List<DocumentChunk> chunks = List.of(
                    createChunk("First chunk", 0),
                    createChunk("Second chunk", 1),
                    createChunk("Third chunk", 2)
            );

            when(embeddingRepository.embed(query)).thenReturn(queryEmbedding);
            when(chunkSearchRepository.search(queryEmbedding, 5)).thenReturn(chunks);

            DocumentSearchService.RetrievalResult result = service.retrieve(query, null, 5);

            assertThat(result.context()).isEqualTo("First chunk\n\nSecond chunk\n\nThird chunk");
        }

        @Test
        @DisplayName("should truncate content longer than 500 characters")
        void shouldTruncateContentLongerThan500Characters() {
            String query = "test";
            float[] queryEmbedding = new float[]{0.1f, 0.2f};
            String longContent = "A".repeat(600);
            DocumentChunk chunk = createChunk(longContent, 0);

            when(embeddingRepository.embed(query)).thenReturn(queryEmbedding);
            when(chunkSearchRepository.search(queryEmbedding, 5)).thenReturn(List.of(chunk));

            DocumentSearchService.RetrievalResult result = service.retrieve(query, null, 5);

            assertThat(result.sources().get(0).text()).hasSize(503); // 500 + "..."
            assertThat(result.sources().get(0).text()).endsWith("...");
        }

        @Test
        @DisplayName("should preserve content shorter than 500 characters")
        void shouldPreserveContentShorterThan500Characters() {
            String query = "test";
            float[] queryEmbedding = new float[]{0.1f, 0.2f};
            String shortContent = "Short content";
            DocumentChunk chunk = createChunk(shortContent, 0);

            when(embeddingRepository.embed(query)).thenReturn(queryEmbedding);
            when(chunkSearchRepository.search(queryEmbedding, 5)).thenReturn(List.of(chunk));

            DocumentSearchService.RetrievalResult result = service.retrieve(query, null, 5);

            assertThat(result.sources().get(0).text()).isEqualTo(shortContent);
        }

        @Test
        @DisplayName("should include metadata in source documents")
        void shouldIncludeMetadataInSourceDocuments() {
            String query = "test";
            float[] queryEmbedding = new float[]{0.1f, 0.2f};
            Map<String, Object> metadata = Map.of("title", "Test Doc", "fileName", "test.txt");
            DocumentChunk chunk = DocumentChunk.reconstitute(
                    DocumentId.generate(), DocumentId.generate(), "Content", 0,
                    metadata, queryEmbedding, Instant.now());

            when(embeddingRepository.embed(query)).thenReturn(queryEmbedding);
            when(chunkSearchRepository.search(queryEmbedding, 5)).thenReturn(List.of(chunk));

            DocumentSearchService.RetrievalResult result = service.retrieve(query, null, 5);

            assertThat(result.sources().get(0).metadata()).containsEntry("title", "Test Doc");
            assertThat(result.sources().get(0).metadata()).containsEntry("fileName", "test.txt");
        }

        @Test
        @DisplayName("should pass empty docIds list to vector adapter")
        void shouldPassEmptyDocIdsListToVectorAdapter() {
            String query = "test";
            float[] queryEmbedding = new float[]{0.1f, 0.2f};

            when(embeddingRepository.embed(query)).thenReturn(queryEmbedding);
            when(chunkSearchRepository.search(queryEmbedding, 5)).thenReturn(List.of());

            service.retrieve(query, List.of(), 5);

            verify(chunkSearchRepository).search(queryEmbedding, 5);
        }

        @Test
        @DisplayName("should calculate similarity score for each source")
        void shouldCalculateSimilarityScoreForEachSource() {
            String query = "test";
            float[] queryEmbedding = new float[]{0.5f, 0.5f};
            float[] chunkEmbedding = new float[]{0.5f, 0.5f};
            DocumentChunk chunk = createChunkWithEmbedding("content", 0, 1.0f);
            DocumentChunk chunkWithEmbedding = DocumentChunk.reconstitute(
                    DocumentId.generate(), DocumentId.generate(), "content", 0,
                    Map.of(), chunkEmbedding, Instant.now());

            when(embeddingRepository.embed(query)).thenReturn(queryEmbedding);
            when(chunkSearchRepository.search(queryEmbedding, 5)).thenReturn(List.of(chunkWithEmbedding));

            DocumentSearchService.RetrievalResult result = service.retrieve(query, null, 5);

            assertThat(result.sources().get(0).score()).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.01));
        }
    }

    private DocumentChunk createChunk(String content, int index) {
        return createChunkWithEmbedding(content, index, 0.8f);
    }

    private DocumentChunk createChunkWithEmbedding(String content, int index, float similarity) {
        float[] queryEmbedding = {0.5f, 0.5f};
        float[] chunkEmbedding = similarity > 0
                ? new float[]{(float) (similarity * 0.7), (float) (similarity * 0.7)}
                : new float[]{0.1f, 0.1f};
        return DocumentChunk.reconstitute(
                DocumentId.generate(), DocumentId.generate(), content, index,
                Map.of(), chunkEmbedding, Instant.now());
    }
}
