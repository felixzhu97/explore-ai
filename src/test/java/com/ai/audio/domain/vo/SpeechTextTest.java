package com.ai.audio.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ai.audio.domain.exception.InvalidSpeechTextException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SpeechText")
class SpeechTextTest {

  @Test
  @DisplayName("should count words")
  void shouldCountWords() {
    assertThat(SpeechText.of("hello world").wordCount()).isEqualTo(2);
  }

  @Test
  @DisplayName("should reject blank text")
  void shouldRejectBlankText() {
    assertThatThrownBy(() -> SpeechText.of(" ")).isInstanceOf(InvalidSpeechTextException.class);
  }

  @Test
  @DisplayName("should reject null via compact constructor")
  void shouldRejectNullViaCompactConstructor() {
    assertThatThrownBy(() -> new SpeechText(null))
        .isInstanceOf(InvalidSpeechTextException.class)
        .hasMessageContaining("blank");
  }

  @Test
  @DisplayName("should reject text exceeding max length")
  void shouldRejectTextExceedingMaxLength() {
    assertThatThrownBy(() -> SpeechText.of("a".repeat(10_001)))
        .isInstanceOf(InvalidSpeechTextException.class)
        .hasMessageContaining("maximum length");
  }
}
