package com.ai.vision.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("YoloImagePreprocessor")
class YoloImagePreprocessorTest {

  @Test
  @DisplayName("should preprocess image into channel first tensor")
  void shouldPreprocessImageIntoChannelFirstTensor() {
    BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);
    for (int y = 0; y < 32; y++) {
      for (int x = 0; x < 32; x++) {
        image.setRGB(x, y, Color.BLUE.getRGB());
      }
    }

    float[] tensor = YoloImagePreprocessor.preprocess(image, 640);

    assertThat(tensor).hasSize(3 * 640 * 640);
    assertThat(tensor[0]).isBetween(0f, 1f);
  }
}
