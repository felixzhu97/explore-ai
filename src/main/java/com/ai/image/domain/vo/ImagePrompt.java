package com.ai.image.domain.vo;

import com.ai.image.domain.exception.InvalidImagePromptException;

public record ImagePrompt(String value) {

    private static final int MAX_LENGTH = 4_000;

    public ImagePrompt {
        if (value == null || value.isBlank()) {
            throw new InvalidImagePromptException("Image prompt must not be blank");
        }
        value = value.trim();
        if (value.length() > MAX_LENGTH) {
            throw new InvalidImagePromptException(
                    "Image prompt exceeds maximum length of " + MAX_LENGTH);
        }
    }

    public static ImagePrompt of(String prompt) {
        return new ImagePrompt(prompt);
    }
}
