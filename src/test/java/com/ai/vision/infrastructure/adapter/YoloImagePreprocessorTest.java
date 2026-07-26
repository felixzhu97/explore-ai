package com.ai.vision.infrastructure.adapter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("YoloImagePreprocessor")
class YoloImagePreprocessorTest {

    @Test
    @DisplayName("should_preprocess_image_into_channel_first_tensor")
    void should_preprocess_image_into_channel_first_tensor() {
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
