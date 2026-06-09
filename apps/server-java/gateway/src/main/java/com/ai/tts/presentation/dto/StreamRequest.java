package com.ai.tts.presentation.dto;

import com.ai.tts.domain.OutputFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StreamRequest(
    @NotBlank(message = "Text is required")
    @Size(min = 1, max = 10000, message = "Text length must be between 1 and 10000")
    String text,

    String voice,

    String language,

    @Min(value = 0, message = "Speed must be at least 0.25")
    @Max(value = 4, message = "Speed must not exceed 4.0")
    float speed,

    OutputFormat outputFormat,

    String provider
) {
    public StreamRequest {
        if (speed == 0) speed = 1.0f;
        if (outputFormat == null) outputFormat = OutputFormat.MP3;
    }
}
