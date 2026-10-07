package com.ai.audio.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ai.common.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SpeechText")
class SpeechTextTest {

  @Test
  @DisplayName("should count words")
  void shouldCountWords() {
    assertThat(SpeechText.of("hello world").countWords()).isEqualTo(2);
  }

  @Test
  @DisplayName("should reject blank text")
  void shouldRejectBlankText() {
    assertThatThrownBy(() -> SpeechText.of(" "))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("code", "INVALID_SPEECH_TEXT");
  }

  @Test
  @DisplayName("should reject null via compact constructor")
  void shouldRejectNullViaCompactConstructor() {
    assertThatThrownBy(() -> new SpeechText(null))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("code", "INVALID_SPEECH_TEXT")
        .hasMessageContaining("blank");
  }

  @Test
  @DisplayName("should reject text exceeding max length")
  void shouldRejectTextExceedingMaxLength() {
    assertThatThrownBy(() -> SpeechText.of("a".repeat(10_001)))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("code", "INVALID_SPEECH_TEXT")
        .hasMessageContaining("maximum length");
  }
}
