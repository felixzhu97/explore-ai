package com.ai.rag.infrastructure;

import com.ai.rag.domain.DocumentChunk;
import com.ai.rag.domain.DocumentChunkSearchRepository;
import com.ai.rag.domain.TextEmbeddingRepository;
import com.ai.rag.domain.DocumentId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("H2SpringAiVectorStore")
class H2SpringAiVectorStoreTest {

    @Mock
    private TextEmbeddingRepository embeddingRepository;

    @Mock
    private DocumentChunkSearchRepository chunkSearchRepository;

    private H2SpringAiVectorStore vectorStore;

    @BeforeEach
    void setUp() {
        vectorStore = new H2SpringAiVectorStore(embeddingRepository, chunkSearchRepository);
    }

    @Test
    @DisplayName("should_returnDocumentsAboveThreshold_when_similaritySearch")
    void should_returnDocumentsAboveThreshold_when_similaritySearch() {
        float[] query = new float[]{1f, 0f};
        float[] high = new float[]{1f, 0f};
        float[] low = new float[]{0f, 1f};
        UUID docId = UUID.randomUUID();
        when(embeddingRepository.embed("apples")).thenReturn(query);
        when(chunkSearchRepository.search(eq(query), eq(2))).thenReturn(List.of(
                chunk(docId, "high", high),
                chunk(docId, "low", low)));

        List<Document> docs = vectorStore.similaritySearch(SearchRequest.builder()
                .query("apples")
                .topK(2)
                .similarityThreshold(0.9)
                .build());

        assertThat(docs).hasSize(1);
        assertThat(docs.getFirst().getText()).isEqualTo("high");
        assertThat(docs.getFirst().getMetadata())
                .containsEntry(H2SpringAiVectorStore.DOCUMENT_ID_METADATA_KEY, docId.toString());
    }

    @Test
    @DisplayName("should_passDocumentIds_when_filterExpressionPresent")
    void should_passDocumentIds_when_filterExpressionPresent() {
        float[] query = new float[]{1f, 0f};
        UUID docA = UUID.randomUUID();
        when(embeddingRepository.embed("q")).thenReturn(query);
        when(chunkSearchRepository.search(any(), anyInt(), any())).thenReturn(List.of());

        var filter = new FilterExpressionBuilder()
                .in(H2SpringAiVectorStore.DOCUMENT_ID_METADATA_KEY, List.of(docA.toString()))
                .build();

        vectorStore.similaritySearch(SearchRequest.builder()
                .query("q")
                .topK(5)
                .filterExpression(filter)
                .build());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UUID>> captor = ArgumentCaptor.forClass(List.class);
        verify(chunkSearchRepository).search(eq(query), eq(5), captor.capture());
        assertThat(captor.getValue()).containsExactly(docA);
    }

    private static DocumentChunk chunk(UUID documentId, String content, float[] embedding) {
        return DocumentChunk.reconstitute(
                DocumentId.generate(),
                DocumentId.of(documentId),
                content,
                0,
                Map.of("title", "t"),
                embedding,
                Instant.now());
    }
}
