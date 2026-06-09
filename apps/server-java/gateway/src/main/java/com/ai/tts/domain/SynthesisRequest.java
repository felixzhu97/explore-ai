package com.ai.tts.domain;

import com.ai.tts.domain.exception.TtsDomainException;

public record SynthesisRequest(
    SpeechId speechId,
    String text,
    String voice,
    String language,
    float speed,
    float pitch,
    OutputFormat outputFormat,
    String provider
) {
    public static SynthesisRequest create(
            String text,
            String voice,
            String language,
            float speed,
            float pitch,
            OutputFormat outputFormat,
            String provider) {

        validateText(text);
        validateSpeed(speed);
        validatePitch(pitch);

        return new SynthesisRequest(
            SpeechId.generate(),
            text,
            voice != null ? voice : "",
            language != null ? language : "zh-CN",
            normalizeSpeed(speed),
            normalizePitch(pitch),
            outputFormat != null ? outputFormat : OutputFormat.MP3,
            provider != null ? provider.toLowerCase() : ""
        );
    }

    private static void validateText(String text) {
        if (text == null || text.isBlank()) {
            throw TtsDomainException.invalidRequest("Text cannot be empty");
        }
    }

    private static void validateSpeed(float speed) {
        if (speed < 0.25f || speed > 4.0f) {
            throw TtsDomainException.invalidRequest("Speed must be between 0.25 and 4.0, got: " + speed);
        }
    }

    private static void validatePitch(float pitch) {
        if (pitch < -20 || pitch > 20) {
            throw TtsDomainException.invalidRequest("Pitch must be between -20 and 20, got: " + pitch);
        }
    }

    private static float normalizeSpeed(float speed) {
        return speed == 0 ? 1.0f : speed;
    }

    private static float normalizePitch(float pitch) {
        return pitch;
    }

    public boolean requiresStreaming() {
        return text.length() > 500;
    }
}
