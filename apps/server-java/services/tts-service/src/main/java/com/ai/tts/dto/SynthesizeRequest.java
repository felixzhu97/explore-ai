package com.ai.tts.dto;

import com.ai.tts.domain.model.OutputFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record SynthesizeRequest(
    @NotBlank(message = "Text is required")
    String text,

    String voice,

    String language,

    @Min(value = 0, message = "Speed must be at least 0")
    @Max(value = 4, message = "Speed must not exceed 4")
    float speed,

    @Min(value = -50, message = "Pitch must be at least -50")
    @Max(value = 50, message = "Pitch must not exceed 50")
    float pitch,

    OutputFormat outputFormat,

    String provider
) {
    public SynthesizeRequest {
        if (speed == 0) speed = 1.0f;
        if (pitch == 0) pitch = 0;
        if (outputFormat == null) outputFormat = OutputFormat.MP3;
    }

    public static SynthesizeRequest of(String text) {
        return new SynthesizeRequest(text, null, null, 1.0f, 0, OutputFormat.MP3, null);
    }
}
