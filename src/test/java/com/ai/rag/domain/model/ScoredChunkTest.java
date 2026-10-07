package com.ai.rag.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.common.domain.model.OwnerKey;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ScoredChunk")
class ScoredChunkTest {

  private static DocumentChunk chunk(float... embedding) {
    return DocumentChunk.create(
            ChunkId.generate(),
            DocumentId.generate(),
            OwnerKey.parse("c:owner"),
            "text",
            0,
            Map.of())
        .withEmbedding(embedding);
  }

  @Test
  @DisplayName("should score the chunk once against the query")
  void shouldScoreTheChunkOnceAgainstTheQuery() {
    ScoredChunk scored = ScoredChunk.of(chunk(1f, 0f), new float[] {1f, 0f});

    assertThat(scored.score()).isEqualTo(1.0);
  }

  @Test
  @DisplayName("should meet a threshold equal to its score")
  void shouldMeetAThresholdEqualToItsScore() {
    ScoredChunk scored = new ScoredChunk(chunk(1f), 0.5);

    assertThat(scored.meets(0.5)).isTrue();
    assertThat(scored.meets(0.51)).isFalse();
  }

  @Test
  @DisplayName("should order the most similar chunk first")
  void shouldOrderTheMostSimilarChunkFirst() {
    ScoredChunk low = new ScoredChunk(chunk(1f), 0.2);
    ScoredChunk high = new ScoredChunk(chunk(1f), 0.9);

    assertThat(List.of(low, high).stream().sorted(ScoredChunk.BEST_FIRST).toList())
        .containsExactly(high, low);
  }
}
