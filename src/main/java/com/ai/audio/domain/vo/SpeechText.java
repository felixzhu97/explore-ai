package com.ai.audio.domain.vo;

import com.ai.audio.domain.exception.InvalidSpeechTextException;

public record SpeechText(String value) {

    private static final int MAX_LENGTH = 10_000;

    public static SpeechText of(String text) {
        if (text == null || text.isBlank()) {
            throw new InvalidSpeechTextException("Speech text must not be blank");
        }
        String trimmed = text.trim();
        if (trimmed.length() > MAX_LENGTH) {
            throw new InvalidSpeechTextException(
                    "Speech text exceeds maximum length of " + MAX_LENGTH);
        }
        return new SpeechText(trimmed);
    }

    public int wordCount() {
        if (value.isBlank()) {
            return 0;
        }
        return value.trim().split("\\s+").length;
    }
}
