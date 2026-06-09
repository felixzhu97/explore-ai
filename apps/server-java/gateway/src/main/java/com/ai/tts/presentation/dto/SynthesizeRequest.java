package com.ai.tts.presentation.dto;

import com.ai.tts.domain.OutputFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SynthesizeRequest(
    @NotBlank(message = "Text is required")
    @Size(min = 1, max = 10000, message = "Text length must be between 1 and 10000")
    String text,

    String voice,

    String language,

    @Min(value = 0, message = "Speed must be at least 0.25")
    @Max(value = 4, message = "Speed must not exceed 4.0")
    float speed,

    @Min(value = -20, message = "Pitch must be at least -20")
    @Max(value = 20, message = "Pitch must not exceed 20")
    float pitch,

    OutputFormat outputFormat,

    AudioConfig audioConfig,

    String provider
) {
    public SynthesizeRequest {
        if (speed == 0) speed = 1.0f;
        if (pitch == 0) pitch = 0;
        if (outputFormat == null) outputFormat = OutputFormat.MP3;
    }

    public static SynthesizeRequest of(String text) {
        return new SynthesizeRequest(text, null, null, 1.0f, 0, OutputFormat.MP3, null, null);
    }

    public record AudioConfig(
        @Min(value = 8000) @Max(value = 48000) int sampleRate,
        int bitRate,
        int channels
    ) {
        public AudioConfig {
            if (sampleRate == 0) sampleRate = 24000;
            if (bitRate == 0) bitRate = 128;
            if (channels == 0) channels = 1;
        }
    }
}
