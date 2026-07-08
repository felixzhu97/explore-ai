package com.ai.audio.domain.vo;

import com.ai.audio.domain.exception.InvalidSpeechTextException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SpeechText")
class SpeechTextTest {

    @Test
    @DisplayName("should count words")
    void should_count_words() {
        assertThat(SpeechText.of("hello world").wordCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("should reject blank text")
    void should_reject_blank_text() {
        assertThatThrownBy(() -> SpeechText.of(" "))
                .isInstanceOf(InvalidSpeechTextException.class);
    }

    @Test
    @DisplayName("should reject null via compact constructor")
    void should_reject_null_via_compact_constructor() {
        assertThatThrownBy(() -> new SpeechText(null))
                .isInstanceOf(InvalidSpeechTextException.class)
                .hasMessageContaining("blank");
    }

    @Test
    @DisplayName("should reject text exceeding max length")
    void should_reject_text_exceeding_max_length() {
        assertThatThrownBy(() -> SpeechText.of("a".repeat(10_001)))
                .isInstanceOf(InvalidSpeechTextException.class)
                .hasMessageContaining("maximum length");
    }
}
