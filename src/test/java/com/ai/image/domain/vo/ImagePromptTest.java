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
}
