package com.ai.rag.infra.llm;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("HashingTextEmbeddingGateway")
class HashingTextEmbeddingGatewayTest {

  @Test
  @DisplayName("should return fixed dimension vector for blank text")
  void shouldReturnFixedDimensionVectorForBlankText() {
    HashingTextEmbeddingGateway repository = new HashingTextEmbeddingGateway(8);

    float[] vector = repository.embedText(" ");

    assertThat(vector).hasSize(8);
    assertThat(repository.getDimensions()).isEqualTo(8);
  }

  @Test
  @DisplayName("should return deterministic vectors for the same text")
  void shouldReturnDeterministicVectorsForTheSameText() {
    HashingTextEmbeddingGateway repository = new HashingTextEmbeddingGateway(16);

    float[] first = repository.embedText("hello");
    float[] second = repository.embedText("hello");

    assertThat(first).containsExactly(second);
  }
}
