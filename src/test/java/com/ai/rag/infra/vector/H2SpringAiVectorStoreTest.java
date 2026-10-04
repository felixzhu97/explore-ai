package com.ai.rag.infra.vector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ai.rag.domain.model.DocumentChunk;
import com.ai.rag.domain.repository.DocumentChunkSearchRepository;
import com.ai.rag.domain.repository.TextEmbeddingGateway;
import com.ai.rag.domain.vo.ChunkId;
import com.ai.rag.domain.vo.DocumentId;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

@ExtendWith(MockitoExtension.class)
@DisplayName("H2SpringAiVectorStore")
class H2SpringAiVectorStoreTest {

  private static final String OWNER = "c:owner";

  @Mock private TextEmbeddingGateway embeddingRepository;

  @Mock private DocumentChunkSearchRepository chunkSearchRepository;

  private H2SpringAiVectorStore vectorStore;

  @BeforeEach
  void setUp() {
    vectorStore = new H2SpringAiVectorStore(embeddingRepository, chunkSearchRepository);
  }

  @Test
  @DisplayName("should return documents above threshold when similarity search")
  void shouldReturnDocumentsAboveThresholdWhenSimilaritySearch() {
    float[] query = new float[] {1f, 0f};
    float[] high = new float[] {1f, 0f};
    float[] low = new float[] {0f, 1f};
    UUID docId = UUID.randomUUID();
    when(embeddingRepository.embed("apples")).thenReturn(query);
    when(chunkSearchRepository.search(eq(query), eq(2), eq(OWNER), eq(List.of())))
        .thenReturn(List.of(chunk(docId, "high", high), chunk(docId, "low", low)));

    List<Document> docs =
        vectorStore.similaritySearch(
            SearchRequest.builder()
                .query("apples")
                .topK(2)
                .similarityThreshold(0.9)
                .filterExpression(ownerFilter().build())
                .build());

    assertThat(docs).hasSize(1);
    assertThat(docs.getFirst().getText()).isEqualTo("high");
    assertThat(docs.getFirst().getMetadata())
        .containsEntry(H2SpringAiVectorStore.DOCUMENT_ID_METADATA_KEY, docId.toString());
  }

  @Test
  @DisplayName("should pass owner and document ids when filter expression has both")
  void shouldPassOwnerAndDocumentIdsWhenFilterExpressionHasBoth() {
    float[] query = new float[] {1f, 0f};
    UUID docA = UUID.randomUUID();
    when(embeddingRepository.embed("q")).thenReturn(query);
    when(chunkSearchRepository.search(any(), anyInt(), any(), any())).thenReturn(List.of());

    FilterExpressionBuilder b = new FilterExpressionBuilder();
    var filter =
        b.and(
                ownerFilter(),
                b.in(H2SpringAiVectorStore.DOCUMENT_ID_METADATA_KEY, List.of(docA.toString())))
            .build();

    vectorStore.similaritySearch(
        SearchRequest.builder().query("q").topK(5).filterExpression(filter).build());

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<UUID>> captor = ArgumentCaptor.forClass(List.class);
    verify(chunkSearchRepository).search(eq(query), eq(5), eq(OWNER), captor.capture());
    assertThat(captor.getValue()).containsExactly(docA);
  }

  @Test
  @DisplayName("should fall back to opening chunks when no selected chunk passes the threshold")
  void shouldFallBackToOpeningChunksWhenNoSelectedChunkPassesTheThreshold() {
    float[] query = new float[] {1f, 0f};
    float[] unrelated = new float[] {0f, 1f};
    UUID docId = UUID.randomUUID();
    when(embeddingRepository.embed("What is this about?")).thenReturn(query);
    when(chunkSearchRepository.search(query, 3, OWNER, List.of(docId)))
        .thenReturn(List.of(chunk(docId, "middle", unrelated)));
    when(chunkSearchRepository.findLeadingChunks(OWNER, List.of(docId), 3))
        .thenReturn(List.of(chunk(docId, "abstract", unrelated)));

    List<Document> docs =
        vectorStore.similaritySearch(
            SearchRequest.builder()
                .query("What is this about?")
                .topK(3)
                .similarityThreshold(0.5)
                .filterExpression(ownerAndDocumentFilter(docId))
                .build());

    assertThat(docs).extracting(Document::getText).containsExactly("abstract");
    assertThat(docs.getFirst().getScore()).isEqualTo(0.0);
  }

  @Test
  @DisplayName("should not fall back when no documents are selected")
  void shouldNotFallBackWhenNoDocumentsAreSelected() {
    float[] query = new float[] {1f, 0f};
    when(embeddingRepository.embed("What is this about?")).thenReturn(query);
    when(chunkSearchRepository.search(query, 3, OWNER, List.of()))
        .thenReturn(List.of(chunk(UUID.randomUUID(), "middle", new float[] {0f, 1f})));

    List<Document> docs =
        vectorStore.similaritySearch(
            SearchRequest.builder()
                .query("What is this about?")
                .topK(3)
                .similarityThreshold(0.5)
                .filterExpression(ownerFilter().build())
                .build());

    assertThat(docs).isEmpty();
    verify(chunkSearchRepository, never()).findLeadingChunks(any(), any(), anyInt());
  }

  @Test
  @DisplayName("should return nothing without searching when the filter has no owner")
  void shouldReturnNothingWithoutSearchingWhenTheFilterHasNoOwner() {
    var documentOnly =
        new FilterExpressionBuilder()
            .in(
                H2SpringAiVectorStore.DOCUMENT_ID_METADATA_KEY,
                List.of(UUID.randomUUID().toString()))
            .build();

    assertThat(vectorStore.similaritySearch(SearchRequest.builder().query("q").build())).isEmpty();
    assertThat(
            vectorStore.similaritySearch(
                SearchRequest.builder().query("q").filterExpression(documentOnly).build()))
        .isEmpty();
    verifyNoInteractions(embeddingRepository, chunkSearchRepository);
  }

  private static FilterExpressionBuilder.Op ownerFilter() {
    return new FilterExpressionBuilder().eq(H2SpringAiVectorStore.OWNER_KEY_METADATA_KEY, OWNER);
  }

  private static Filter.Expression ownerAndDocumentFilter(UUID documentId) {
    FilterExpressionBuilder b = new FilterExpressionBuilder();
    return b.and(
            ownerFilter(),
            b.in(H2SpringAiVectorStore.DOCUMENT_ID_METADATA_KEY, List.of(documentId.toString())))
        .build();
  }

  private static DocumentChunk chunk(UUID documentId, String content, float[] embedding) {
    return DocumentChunk.reconstitute(
        ChunkId.generate(),
        DocumentId.of(documentId),
        content,
        0,
        Map.of("title", "t"),
        embedding,
        Instant.now());
  }
}
