package com.ai.tts.dto;

import com.ai.tts.domain.model.OutputFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record StreamRequest(
    @NotBlank(message = "Text is required")
    String text,

    String voice,

    String language,

    @Min(value = 0, message = "Speed must be at least 0")
    @Max(value = 4, message = "Speed must not exceed 4")
    float speed,

    OutputFormat outputFormat,

    String provider
) {
    public StreamRequest {
        if (speed == 0) speed = 1.0f;
        if (outputFormat == null) outputFormat = OutputFormat.MP3;
    }
}
