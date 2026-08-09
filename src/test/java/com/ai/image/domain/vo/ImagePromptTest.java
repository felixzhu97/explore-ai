package com.ai.image.domain.vo;

import com.ai.image.domain.exception.InvalidImagePromptException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ImagePrompt")
class ImagePromptTest {

    @Test
    @DisplayName("should reject blank prompt")
    void shouldRejectBlankPrompt() {
        assertThatThrownBy(() -> ImagePrompt.of(" "))
                .isInstanceOf(InvalidImagePromptException.class);
    }

    @Test
    @DisplayName("should reject null via compact constructor")
    void shouldRejectNullViaCompactConstructor() {
        assertThatThrownBy(() -> new ImagePrompt(null))
                .isInstanceOf(InvalidImagePromptException.class)
                .hasMessageContaining("blank");
    }

    @Test
    @DisplayName("should reject prompt exceeding max length")
    void shouldRejectPromptExceedingMaxLength() {
        assertThatThrownBy(() -> ImagePrompt.of("a".repeat(4_001)))
                .isInstanceOf(InvalidImagePromptException.class)
                .hasMessageContaining("maximum length");
    }
}
