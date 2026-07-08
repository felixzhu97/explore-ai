package com.ai.image.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GeneratedImage")
class GeneratedImageTest {

    @Test
    @DisplayName("should expose availability")
    void should_expose_availability() {
        assertThat(GeneratedImage.create("https://x", "dall-e-3", "cat").isAvailable()).isTrue();
        assertThat(GeneratedImage.empty().hasUrl()).isFalse();
    }
}
