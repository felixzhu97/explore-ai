package com.ai.image.domain.vo;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ai.common.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ImagePrompt")
class ImagePromptTest {

  @Test
  @DisplayName("should reject blank prompt")
  void shouldRejectBlankPrompt() {
    assertThatThrownBy(() -> ImagePrompt.of(" "))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("code", "INVALID_IMAGE_PROMPT");
  }

  @Test
  @DisplayName("should reject null via compact constructor")
  void shouldRejectNullViaCompactConstructor() {
    assertThatThrownBy(() -> new ImagePrompt(null))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("code", "INVALID_IMAGE_PROMPT")
        .hasMessageContaining("blank");
  }

  @Test
  @DisplayName("should reject prompt exceeding max length")
  void shouldRejectPromptExceedingMaxLength() {
    assertThatThrownBy(() -> ImagePrompt.of("a".repeat(4_001)))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("code", "INVALID_IMAGE_PROMPT")
        .hasMessageContaining("maximum length");
  }
}
