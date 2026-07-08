package com.ai.audio.domain.vo;

import com.ai.audio.domain.exception.InvalidSpeechTextException;

public record SpeechText(String value) {

    private static final int MAX_LENGTH = 10_000;

    public SpeechText {
        if (value == null || value.isBlank()) {
            throw new InvalidSpeechTextException("Speech text must not be blank");
        }
        value = value.trim();
        if (value.length() > MAX_LENGTH) {
            throw new InvalidSpeechTextException(
                    "Speech text exceeds maximum length of " + MAX_LENGTH);
        }
    }

    public static SpeechText of(String text) {
        return new SpeechText(text);
    }

    public int wordCount() {
        if (value.isBlank()) {
            return 0;
        }
        return value.trim().split("\\s+").length;
    }
}
