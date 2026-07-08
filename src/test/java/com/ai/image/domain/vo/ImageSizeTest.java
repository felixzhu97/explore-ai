package com.ai.image.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ImageSize")
class ImageSizeTest {

    @Test
    @DisplayName("should accept supported size")
    void should_accept_supported_size() {
        ImageSize size = ImageSize.of(1024, 1024);

        assertThat(size.isSupported()).isTrue();
    }
}
