package com.ai.rag.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.ai.common.domain.model.OwnerKey;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("DocumentChunk")
class DocumentChunkTest {

  private static final ChunkId TEST_ID = ChunkId.parseId("123e4567-e89b-12d3-a456-426614174000");
  private static final DocumentId TEST_DOCUMENT_ID =
      DocumentId.createId(UUID.fromString("223e4567-e89b-12d3-a456-426614174001"));
  private static final OwnerKey OWNER = OwnerKey.parseKey("c:owner");
  private static final String TEST_CONTENT = "This is a test chunk content.";
  private static final int TEST_CHUNK_INDEX = 0;

  private static DocumentChunk chunk(String content, Map<String, Object> metadata) {
    return DocumentChunk.createChunk(
        TEST_ID, TEST_DOCUMENT_ID, OWNER, content, TEST_CHUNK_INDEX, metadata);
  }

  private static DocumentChunk chunk() {
    return chunk(TEST_CONTENT, Map.of());
  }

  @Nested
  @DisplayName("Creation")
  class Creation {

    @Test
    @DisplayName("should create chunk with all fields and no embedding")
    void shouldCreateChunkWithAllFieldsAndNoEmbedding() {
      Instant before = Instant.now();
      Map<String, Object> metadata = Map.of("author", "test-author");

      DocumentChunk chunk = chunk(TEST_CONTENT, metadata);

      assertThat(chunk.getId()).isEqualTo(TEST_ID);
      assertThat(chunk.getDocumentId()).isEqualTo(TEST_DOCUMENT_ID);
      assertThat(chunk.getOwnerKey()).isEqualTo(OWNER);
      assertThat(chunk.getContent()).isEqualTo(TEST_CONTENT);
      assertThat(chunk.getChunkIndex()).isEqualTo(TEST_CHUNK_INDEX);
      assertThat(chunk.getMetadata()).isEqualTo(metadata);
      assertThat(chunk.getEmbedding()).isNull();
      assertThat(chunk.getCreatedAt()).isAfterOrEqualTo(before);
    }

    @Test
    @DisplayName("should drop the owner from metadata when stored rows still carry it")
    void shouldDropTheOwnerFromMetadataWhenStoredRowsStillCarryIt() {
      DocumentChunk chunk = chunk(TEST_CONTENT, Map.of("ownerKey", "c:owner", "page", 1));

      assertThat(chunk.getMetadata()).containsOnlyKeys("page");
    }

    @Test
    @DisplayName("should keep its own copy of metadata")
    void shouldKeepItsOwnCopyOfMetadata() {
      Map<String, Object> metadata = new HashMap<>();
      metadata.put("key", "original");
      DocumentChunk chunk = chunk(TEST_CONTENT, metadata);

      metadata.put("newKey", "newValue");

      assertThat(chunk.getMetadata()).doesNotContainKey("newKey");
    }

    @Test
    @DisplayName("should reject missing id document owner or content")
    void shouldRejectMissingIdDocumentOwnerOrContent() {
      assertThatThrownBy(
              () ->
                  DocumentChunk.createChunk(
                      null, TEST_DOCUMENT_ID, OWNER, TEST_CONTENT, 0, Map.of()))
          .isInstanceOf(NullPointerException.class);
      assertThatThrownBy(
              () -> DocumentChunk.createChunk(TEST_ID, null, OWNER, TEST_CONTENT, 0, Map.of()))
          .isInstanceOf(NullPointerException.class);
      assertThatThrownBy(
              () ->
                  DocumentChunk.createChunk(
                      TEST_ID, TEST_DOCUMENT_ID, null, TEST_CONTENT, 0, Map.of()))
          .isInstanceOf(NullPointerException.class);
      assertThatThrownBy(() -> chunk(null, Map.of())).isInstanceOf(NullPointerException.class);
    }
  }

  @Test
  @DisplayName("should return a new chunk with the embedding and keep the original unchanged")
  void shouldReturnANewChunkWithTheEmbeddingAndKeepTheOriginalUnchanged() {
    DocumentChunk original = chunk(TEST_CONTENT, Map.of("key", "value"));
    float[] embedding = {0.1f, 0.2f, 0.3f};

    DocumentChunk embedded = original.copyWithEmbedding(embedding);

    assertThat(embedded.getEmbedding()).containsExactly(0.1f, 0.2f, 0.3f);
    assertThat(embedded.getId()).isEqualTo(original.getId());
    assertThat(embedded.getOwnerKey()).isEqualTo(OWNER);
    assertThat(embedded.getMetadata()).isEqualTo(original.getMetadata());
    assertThat(embedded.getCreatedAt()).isEqualTo(original.getCreatedAt());
    assertThat(original.getEmbedding()).isNull();
  }

  @Test
  @DisplayName("should not change when the caller edits the embedding arrays")
  void shouldNotChangeWhenTheCallerEditsTheEmbeddingArrays() {
    float[] embedding = {0.1f, 0.2f};
    DocumentChunk embedded = chunk().copyWithEmbedding(embedding);

    embedding[0] = 9f;
    embedded.getEmbedding()[1] = 9f;

    assertThat(embedded.getEmbedding()).containsExactly(0.1f, 0.2f);
  }

  @Nested
  @DisplayName("similarityTo()")
  class SimilarityTo {

    @Test
    @DisplayName("should score identical directions as one")
    void shouldScoreIdenticalDirectionsAsOne() {
      DocumentChunk embedded = chunk().copyWithEmbedding(new float[] {1f, 0f});

      assertThat(embedded.calculateSimilarity(new float[] {2f, 0f})).isEqualTo(1.0);
      assertThat(embedded.isComparableWith(new float[] {2f, 0f})).isTrue();
    }

    @Test
    @DisplayName("should score zero when the chunk has no comparable embedding")
    void shouldScoreZeroWhenTheChunkHasNoComparableEmbedding() {
      DocumentChunk embedded = chunk().copyWithEmbedding(new float[] {1f, 0f});

      assertThat(chunk().calculateSimilarity(new float[] {1f, 0f})).isZero();
      assertThat(chunk().isComparableWith(new float[] {1f, 0f})).isFalse();
      assertThat(embedded.calculateSimilarity(new float[] {1f, 0f, 0f})).isZero();
      assertThat(embedded.isComparableWith(new float[] {1f, 0f, 0f})).isFalse();
    }

    private double similarity(float[] chunkEmbedding, float[] queryEmbedding) {
      return chunk().copyWithEmbedding(chunkEmbedding).calculateSimilarity(queryEmbedding);
    }

    @Test
    @DisplayName("should return 1 when the query points the same way")
    void shouldReturnOneWhenTheQueryPointsTheSameWay() {
      assertThat(similarity(new float[] {1f, -2f, 3f}, new float[] {2f, -4f, 6f}))
          .isCloseTo(1.0, within(0.0001));
    }

    @Test
    @DisplayName("should return -1 when the query points the opposite way")
    void shouldReturnMinusOneWhenTheQueryPointsTheOppositeWay() {
      assertThat(similarity(new float[] {1f, 2f, 3f}, new float[] {-1f, -2f, -3f}))
          .isCloseTo(-1.0, within(0.0001));
    }

    @Test
    @DisplayName("should return 0 when the query is orthogonal")
    void shouldReturnZeroWhenTheQueryIsOrthogonal() {
      assertThat(similarity(new float[] {1f, 0f}, new float[] {0f, 1f}))
          .isCloseTo(0.0, within(0.0001));
    }

    @Test
    @DisplayName("should return 0 when either vector is all zeros")
    void shouldReturnZeroWhenEitherVectorIsAllZeros() {
      assertThat(similarity(new float[] {0f, 0f}, new float[] {0f, 0f})).isZero();
    }

    @Test
    @DisplayName("should return 0 when the chunk has no embedding or sizes differ")
    void shouldReturnZeroWhenTheChunkHasNoEmbeddingOrSizesDiffer() {
      assertThat(chunk().calculateSimilarity(new float[] {1f, 2f})).isZero();
      assertThat(similarity(new float[] {1f, 2f}, null)).isZero();
      assertThat(similarity(new float[] {1f, 2f, 3f}, new float[] {1f, 2f})).isZero();
    }
  }

  @Test
  @DisplayName("should keep short content whole")
  void shouldKeepShortContentWhole() {
    assertThat(chunk().getExcerpt()).isEqualTo(TEST_CONTENT);
  }

  @Test
  @DisplayName("should cut long content with an ellipsis")
  void shouldCutLongContentWithAnEllipsis() {
    String excerpt = chunk("x".repeat(DocumentChunk.EXCERPT_LENGTH + 10), Map.of()).getExcerpt();

    assertThat(excerpt).hasSize(DocumentChunk.EXCERPT_LENGTH + 3).endsWith("...");
  }
}
