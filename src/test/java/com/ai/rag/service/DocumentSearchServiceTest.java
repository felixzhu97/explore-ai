package com.ai.rag.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ai.common.domain.model.OwnerKey;
import com.ai.rag.domain.model.ChunkId;
import com.ai.rag.domain.model.DocumentChunk;
import com.ai.rag.domain.model.DocumentId;
import com.ai.rag.domain.model.ScoredChunk;
import com.ai.rag.domain.model.SourceDocument;
import com.ai.rag.domain.repository.DocumentChunkSearchRepository;
import com.ai.rag.domain.repository.RagRetrievalSettings;
import com.ai.rag.domain.repository.TextEmbeddingGateway;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentSearchService")
class DocumentSearchServiceTest {

  private static final String OWNER = "c:owner";
  private static final float[] QUERY_EMBEDDING = {0.1f, 0.2f};

  @Mock private TextEmbeddingGateway embeddingRepository;

  @Mock private DocumentChunkSearchRepository chunkSearchRepository;

  @Mock private RagRetrievalSettings retrievalSettings;

  private DocumentSearchService service;

  @BeforeEach
  void setUp() {
    lenient().when(retrievalSettings.getTopK()).thenReturn(5);
    lenient().when(retrievalSettings.getScoreThreshold()).thenReturn(0.0);
    lenient().when(embeddingRepository.embed(any())).thenReturn(QUERY_EMBEDDING);

    service =
        new DocumentSearchService(embeddingRepository, chunkSearchRepository, retrievalSettings);
  }

  @Nested
  @DisplayName("retrieve()")
  class Retrieve {

    @Test
    @DisplayName("should retrieve the owner chunks when no documents are selected")
    void shouldRetrieveTheOwnerChunksWhenNoDocumentsAreSelected() {
      when(chunkSearchRepository.search(QUERY_EMBEDDING, 5, OWNER, List.of()))
          .thenReturn(
              List.of(
                  scored("AI stands for Artificial Intelligence", 0.9),
                  scored("Machine learning is a subset of AI", 0.8)));

      DocumentSearchService.RetrievalResult result =
          service.retrieve("What is AI?", null, 5, OWNER);

      assertThat(result.context())
          .contains("AI stands for Artificial Intelligence")
          .contains("Machine learning is a subset of AI");
      assertThat(result.sources()).hasSize(2);
    }

    @Test
    @DisplayName("should search only the selected documents when ids are given")
    void shouldSearchOnlyTheSelectedDocumentsWhenIdsAreGiven() {
      DocumentId docId = DocumentId.generate();
      when(chunkSearchRepository.search(QUERY_EMBEDDING, 5, OWNER, List.of(docId.getValue())))
          .thenReturn(List.of(scored("filtered content", 0.7)));

      DocumentSearchService.RetrievalResult result =
          service.retrieve("test query", List.of(docId), 5, OWNER);

      assertThat(result.sources()).hasSize(1);
    }

    @Test
    @DisplayName("should use the default topK when none is given")
    void shouldUseTheDefaultTopKWhenNoneIsGiven() {
      when(chunkSearchRepository.search(QUERY_EMBEDDING, 5, OWNER, List.of()))
          .thenReturn(List.of());

      service.retrieve("test", List.of(), 0, OWNER);

      verify(chunkSearchRepository).search(QUERY_EMBEDDING, 5, OWNER, List.of());
    }

    @Test
    @DisplayName("should use the given topK when it is positive")
    void shouldUseTheGivenTopKWhenItIsPositive() {
      when(chunkSearchRepository.search(QUERY_EMBEDDING, 10, OWNER, List.of()))
          .thenReturn(List.of());

      service.retrieve("test", null, 10, OWNER);

      verify(chunkSearchRepository).search(QUERY_EMBEDDING, 10, OWNER, List.of());
    }

    @Test
    @DisplayName("should order sources by score and keep the repository score")
    void shouldOrderSourcesByScoreAndKeepTheRepositoryScore() {
      when(chunkSearchRepository.search(any(), anyInt(), any(), any()))
          .thenReturn(List.of(scored("low", 0.2), scored("high", 0.9), scored("medium", 0.5)));

      DocumentSearchService.RetrievalResult result = service.retrieve("test", null, 5, OWNER);

      assertThat(result.sources()).extracting(SourceDocument::score).containsExactly(0.9, 0.5, 0.2);
      assertThat(result.context()).isEqualTo("high\n\nmedium\n\nlow");
    }

    @Test
    @DisplayName("should drop chunks below the score threshold")
    void shouldDropChunksBelowTheScoreThreshold() {
      when(retrievalSettings.getScoreThreshold()).thenReturn(0.5);
      when(chunkSearchRepository.search(any(), anyInt(), any(), any()))
          .thenReturn(List.of(scored("kept", 0.5), scored("dropped", 0.49)));

      DocumentSearchService.RetrievalResult result = service.retrieve("test", null, 5, OWNER);

      assertThat(result.sources()).extracting(SourceDocument::content).containsExactly("kept");
      assertThat(result.context()).isEqualTo("kept");
    }

    @Test
    @DisplayName("should return an empty result when nothing matches")
    void shouldReturnAnEmptyResultWhenNothingMatches() {
      when(chunkSearchRepository.search(QUERY_EMBEDDING, 5, OWNER, List.of()))
          .thenReturn(List.of());

      DocumentSearchService.RetrievalResult result =
          service.retrieve("nonexistent topic", null, 5, OWNER);

      assertThat(result.context()).isEmpty();
      assertThat(result.sources()).isEmpty();
    }

    @Test
    @DisplayName("should cite an excerpt of long chunks and keep the full text as context")
    void shouldCiteAnExcerptOfLongChunksAndKeepTheFullTextAsContext() {
      String longContent = "A".repeat(600);
      when(chunkSearchRepository.search(QUERY_EMBEDDING, 5, OWNER, List.of()))
          .thenReturn(List.of(scored(longContent, 0.8)));

      DocumentSearchService.RetrievalResult result = service.retrieve("test", null, 5, OWNER);

      assertThat(result.sources().get(0).content()).hasSize(503).endsWith("...");
      assertThat(result.context()).isEqualTo(longContent);
    }

    @Test
    @DisplayName("should include chunk metadata in sources")
    void shouldIncludeChunkMetadataInSources() {
      Map<String, Object> metadata = Map.of("title", "Test Doc", "fileName", "test.txt");
      when(chunkSearchRepository.search(QUERY_EMBEDDING, 5, OWNER, List.of()))
          .thenReturn(List.of(new ScoredChunk(chunk("Content", metadata), 0.8)));

      DocumentSearchService.RetrievalResult result = service.retrieve("test", null, 5, OWNER);

      assertThat(result.sources().get(0).metadata())
          .containsEntry("title", "Test Doc")
          .containsEntry("fileName", "test.txt");
    }
  }

  private static ScoredChunk scored(String content, double score) {
    return new ScoredChunk(chunk(content, Map.of()), score);
  }

  private static DocumentChunk chunk(String content, Map<String, Object> metadata) {
    return DocumentChunk.create(
            ChunkId.generate(), DocumentId.generate(), OwnerKey.parse(OWNER), content, 0, metadata)
        .withEmbedding(QUERY_EMBEDDING);
  }
}
