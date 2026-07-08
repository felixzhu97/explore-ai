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
}
