package com.ai.vision.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BlipVocabulary")
class BlipVocabularyTest {

  @Test
  @DisplayName("should load and decode vocabulary")
  void shouldLoadAndDecodeVocabulary() throws Exception {
    Path tokenizerDir = Files.createTempDirectory("blip-tokenizer");
    Files.writeString(
        tokenizerDir.resolve("vocab.txt"),
        """
                [PAD]
                [DEC]
                hello
                ##world
                """);

    BlipVocabulary vocabulary = BlipVocabulary.load(tokenizerDir);
    String decoded = vocabulary.decode(List.of(101L, 2L, 3L, 102L));

    assertThat(decoded).isEqualTo("helloworld");
    assertThat(vocabulary.decoderStartTokenId()).isEqualTo(1);
    assertThat(vocabulary.eosTokenId()).isEqualTo(102);
  }

  @Test
  @DisplayName("should throw when vocab file missing")
  void shouldThrowWhenVocabFileMissing() throws Exception {
    Path tokenizerDir = Files.createTempDirectory("missing-vocab");

    assertThatThrownBy(() -> BlipVocabulary.load(tokenizerDir))
        .isInstanceOf(java.io.IOException.class)
        .hasMessageContaining("vocab.txt not found");
  }
}
