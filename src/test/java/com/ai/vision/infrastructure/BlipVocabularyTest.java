package com.ai.vision.infrastructure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BlipVocabulary")
class BlipVocabularyTest {

    @Test
    @DisplayName("should_load_and_decode_vocabulary")
    void should_load_and_decode_vocabulary() throws Exception {
        Path tokenizerDir = Files.createTempDirectory("blip-tokenizer");
        Files.writeString(tokenizerDir.resolve("vocab.txt"), """
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
    @DisplayName("should_throw_when_vocab_file_missing")
    void should_throw_when_vocab_file_missing() throws Exception {
        Path tokenizerDir = Files.createTempDirectory("missing-vocab");

        assertThatThrownBy(() -> BlipVocabulary.load(tokenizerDir))
                .isInstanceOf(java.io.IOException.class)
                .hasMessageContaining("vocab.txt not found");
    }
}
