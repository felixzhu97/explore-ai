package com.ai.image.domain.vo;

import com.ai.image.domain.exception.InvalidImagePromptException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ImagePrompt")
class ImagePromptTest {

    @Test
    @DisplayName("should reject blank prompt")
    void should_reject_blank_prompt() {
        assertThatThrownBy(() -> ImagePrompt.of(" "))
                .isInstanceOf(InvalidImagePromptException.class);
    }

    @Test
    @DisplayName("should reject null via compact constructor")
    void should_reject_null_via_compact_constructor() {
        assertThatThrownBy(() -> new ImagePrompt(null))
                .isInstanceOf(InvalidImagePromptException.class)
                .hasMessageContaining("blank");
    }

    @Test
    @DisplayName("should reject prompt exceeding max length")
    void should_reject_prompt_exceeding_max_length() {
        assertThatThrownBy(() -> ImagePrompt.of("a".repeat(4_001)))
                .isInstanceOf(InvalidImagePromptException.class)
                .hasMessageContaining("maximum length");
    }
}
