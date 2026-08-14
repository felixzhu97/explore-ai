package com.ai.rag.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;

@DisplayName("RagEtlConfig")
class RagEtlConfigTest {

  @Test
  @DisplayName("should build TokenTextSplitter from rag chunk size")
  void shouldBuildTokenTextSplitterFromRagChunkSize() {
    RagProperties properties = new RagProperties();
    properties.getChunk().setSize(512);

    TokenTextSplitter splitter = new RagEtlConfig().ragTokenTextSplitter(properties);

    assertThat(splitter).isNotNull();
    assertThat(splitter.split(new org.springframework.ai.document.Document("word ".repeat(2000))))
        .isNotEmpty();
  }
}
