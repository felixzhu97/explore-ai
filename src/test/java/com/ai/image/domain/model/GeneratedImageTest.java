package com.ai.image.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GeneratedImage")
class GeneratedImageTest {

    @Test
    @DisplayName("should expose availability for url and base64")
    void should_expose_availability_for_url_and_base64() {
        assertThat(GeneratedImage.fromUrl("https://x", "dall-e-3", "cat").isAvailable()).isTrue();
        assertThat(GeneratedImage.fromBase64("abc", "dall-e-3", "cat").isAvailable()).isTrue();
        assertThat(GeneratedImage.empty().hasUrl()).isFalse();
        assertThat(GeneratedImage.empty().hasBase64()).isFalse();
    }
}
