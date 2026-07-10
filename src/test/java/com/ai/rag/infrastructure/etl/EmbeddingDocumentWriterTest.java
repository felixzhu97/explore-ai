package com.ai.rag.infrastructure.etl;

import com.ai.rag.domain.model.DocumentChunk;
import com.ai.rag.domain.repository.IDocumentChunkRepository;
import com.ai.rag.domain.vo.DocumentId;
import com.ai.rag.infrastructure.llm.EmbeddingAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmbeddingDocumentWriter")
class EmbeddingDocumentWriterTest {

    private static final DocumentId DOCUMENT_ID = DocumentId.of("123e4567-e89b-12d3-a456-426614174000");

    @Mock
    private EmbeddingAdapter embeddingAdapter;

    @Mock
    private IDocumentChunkRepository chunkRepository;

    private EmbeddingDocumentWriter writer;

    @BeforeEach
    void setUp() {
        writer = new EmbeddingDocumentWriter(embeddingAdapter, chunkRepository);
    }

    @Test
    @DisplayName("should embed each chunk content before saving")
    void should_embed_each_chunk_content_before_saving() {
        DocumentChunk firstChunk = createChunk("first chunk", 0);
        DocumentChunk secondChunk = createChunk("second chunk", 1);
        float[] firstEmbedding = new float[]{0.1f, 0.2f};
        float[] secondEmbedding = new float[]{0.3f, 0.4f};
        when(embeddingAdapter.embed("first chunk")).thenReturn(firstEmbedding);
        when(embeddingAdapter.embed("second chunk")).thenReturn(secondEmbedding);

        writer.write(List.of(firstChunk, secondChunk));

        ArgumentCaptor<DocumentChunk> chunkCaptor = ArgumentCaptor.forClass(DocumentChunk.class);
        verify(chunkRepository, times(2)).saveChunk(chunkCaptor.capture());
        assertThat(chunkCaptor.getAllValues())
                .extracting(DocumentChunk::getContent)
                .containsExactly("first chunk", "second chunk");
        assertThat(chunkCaptor.getAllValues().get(0).getEmbedding()).isSameAs(firstEmbedding);
        assertThat(chunkCaptor.getAllValues().get(1).getEmbedding()).isSameAs(secondEmbedding);
        assertThat(firstChunk.getEmbedding()).isNull();
        assertThat(secondChunk.getEmbedding()).isNull();
        verify(embeddingAdapter).embed("first chunk");
        verify(embeddingAdapter).embed("second chunk");
    }

    @Test
    @DisplayName("should not call embedder or repository when no chunks are provided")
    void should_not_call_embedder_or_repository_when_no_chunks_are_provided() {
        writer.write(List.of());

        verifyNoInteractions(embeddingAdapter, chunkRepository);
    }

    private DocumentChunk createChunk(String content, int chunkIndex) {
        return DocumentChunk.create(DocumentId.generate(), DOCUMENT_ID, content, chunkIndex, Map.of("source", "guide.txt"));
    }
}
