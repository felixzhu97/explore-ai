package com.ai.rag.infra.etl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ai.common.domain.model.OwnerKey;
import com.ai.rag.domain.model.ChunkId;
import com.ai.rag.domain.model.DocumentChunk;
import com.ai.rag.domain.model.DocumentId;
import com.ai.rag.domain.repository.DocumentChunkRepository;
import com.ai.rag.domain.repository.TextEmbeddingGateway;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmbeddingDocumentWriter")
class EmbeddingDocumentWriterTest {

  private static final DocumentId DOCUMENT_ID =
      DocumentId.parseId("123e4567-e89b-12d3-a456-426614174000");

  @Mock private TextEmbeddingGateway embeddingRepository;

  @Mock private DocumentChunkRepository chunkRepository;

  private EmbeddingDocumentWriter writer;

  @BeforeEach
  void setUp() {
    writer = new EmbeddingDocumentWriter(embeddingRepository, chunkRepository);
  }

  @Test
  @DisplayName("should embed each chunk content before saving")
  void shouldEmbedEachChunkContentBeforeSaving() {
    DocumentChunk firstChunk = createChunk("first chunk", 0);
    DocumentChunk secondChunk = createChunk("second chunk", 1);
    float[] firstEmbedding = new float[] {0.1f, 0.2f};
    float[] secondEmbedding = new float[] {0.3f, 0.4f};
    when(embeddingRepository.embedText("first chunk")).thenReturn(firstEmbedding);
    when(embeddingRepository.embedText("second chunk")).thenReturn(secondEmbedding);

    writer.writeChunks(List.of(firstChunk, secondChunk));

    ArgumentCaptor<DocumentChunk> chunkCaptor = ArgumentCaptor.forClass(DocumentChunk.class);
    verify(chunkRepository, times(2)).saveChunk(chunkCaptor.capture());
    assertThat(chunkCaptor.getAllValues())
        .extracting(DocumentChunk::getContent)
        .containsExactly("first chunk", "second chunk");
    assertThat(chunkCaptor.getAllValues().get(0).getEmbedding()).containsExactly(firstEmbedding);
    assertThat(chunkCaptor.getAllValues().get(1).getEmbedding()).containsExactly(secondEmbedding);
    assertThat(firstChunk.getEmbedding()).isNull();
    assertThat(secondChunk.getEmbedding()).isNull();
  }

  @Test
  @DisplayName("should not call embedder or repository when no chunks are provided")
  void shouldNotCallEmbedderOrRepositoryWhenNoChunksAreProvided() {
    writer.writeChunks(List.of());

    verifyNoInteractions(embeddingRepository, chunkRepository);
  }

  private DocumentChunk createChunk(String content, int chunkIndex) {
    return DocumentChunk.createChunk(
        ChunkId.generateId(),
        DOCUMENT_ID,
        OwnerKey.parseKey("c:owner"),
        content,
        chunkIndex,
        Map.of("source", "guide.txt"));
  }
}
