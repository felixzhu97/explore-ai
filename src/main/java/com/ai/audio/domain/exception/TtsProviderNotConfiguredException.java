package com.ai.audio.domain.exception;

public class TtsProviderNotConfiguredException extends RuntimeException {

    public TtsProviderNotConfiguredException(String message) {
        super(message);
    }

    public static TtsProviderNotConfiguredException disabled() {
        return new TtsProviderNotConfiguredException("Text-to-speech is disabled");
    }

    public static TtsProviderNotConfiguredException apiKeyMissing() {
        return new TtsProviderNotConfiguredException(
                "TTS provider not configured. Set OPENAI_API_KEY or TTS_API_KEY");
    }
}
