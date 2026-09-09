package com.ai.rag.infra.llm;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("HashingTextEmbeddingRepository")
class HashingTextEmbeddingRepositoryTest {

  @Test
  @DisplayName("should return fixed dimension vector for blank text")
  void shouldReturnFixedDimensionVectorForBlankText() {
    HashingTextEmbeddingRepository repository = new HashingTextEmbeddingRepository(8);

    float[] vector = repository.embed(" ");

    assertThat(vector).hasSize(8);
    assertThat(repository.getDimensions()).isEqualTo(8);
  }

  @Test
  @DisplayName("should return deterministic vectors for the same text")
  void shouldReturnDeterministicVectorsForTheSameText() {
    HashingTextEmbeddingRepository repository = new HashingTextEmbeddingRepository(16);

    float[] first = repository.embed("hello");
    float[] second = repository.embed("hello");

    assertThat(first).containsExactly(second);
  }
}
