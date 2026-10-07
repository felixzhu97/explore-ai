package com.ai.image.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ImageSize")
class ImageSizeTest {

  @Test
  @DisplayName("should accept supported size")
  void shouldAcceptSupportedSize() {
    ImageSize size = ImageSize.createSize(1024, 1024);

    assertThat(size.isSupported()).isTrue();
  }
}
