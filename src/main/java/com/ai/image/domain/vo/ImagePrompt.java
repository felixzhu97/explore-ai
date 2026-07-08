package com.ai.image.domain.vo;

import com.ai.image.domain.exception.InvalidImagePromptException;

public record ImagePrompt(String value) {

    private static final int MAX_LENGTH = 4_000;

    public static ImagePrompt of(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new InvalidImagePromptException("Image prompt must not be blank");
        }
        String trimmed = prompt.trim();
        if (trimmed.length() > MAX_LENGTH) {
            throw new InvalidImagePromptException(
                    "Image prompt exceeds maximum length of " + MAX_LENGTH);
        }
        return new ImagePrompt(trimmed);
    }
}
