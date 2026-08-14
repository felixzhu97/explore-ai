package com.ai.vision.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BlipImagePreprocessor")
class BlipImagePreprocessorTest {

  @Test
  @DisplayName("should preprocess image into normalized tensor")
  void shouldPreprocessImageIntoNormalizedTensor() {
    BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
    for (int y = 0; y < 64; y++) {
      for (int x = 0; x < 64; x++) {
        image.setRGB(x, y, Color.RED.getRGB());
      }
    }

    float[] tensor = BlipImagePreprocessor.preprocess(image);

    assertThat(tensor).hasSize(3 * 384 * 384);
    assertThat(tensor[0]).isNotEqualTo(0f);
  }
}
