package com.ai.image.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GeneratedImage")
class GeneratedImageTest {

  @Test
  @DisplayName("should expose availability for url and base64")
  void shouldExposeAvailabilityForUrlAndBase64() {
    assertThat(GeneratedImage.createUrlImage("https://x", "dall-e-3", "cat").isAvailable())
        .isTrue();
    assertThat(GeneratedImage.createBase64Image("abc", "dall-e-3", "cat").isAvailable()).isTrue();
    assertThat(GeneratedImage.createEmptyImage().hasUrl()).isFalse();
    assertThat(GeneratedImage.createEmptyImage().hasBase64()).isFalse();
  }
}
