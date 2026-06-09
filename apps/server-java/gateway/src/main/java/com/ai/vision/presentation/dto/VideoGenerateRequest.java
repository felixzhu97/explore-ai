package com.ai.vision.presentation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record VideoGenerateRequest(
    @Size(min = 1, max = 500) String prompt,
    @Size(max = 500) String negativePrompt,
    @Min(5) @Max(10) int duration,
    String aspectRatio,
    @Min(24) @Max(60) int fps,
    String quality,
    String model,
    String callbackUrl,
    String style,
    Integer seed,
    @Min(1) @Max(20) float cfgScale,
    @DecimalMin("0.1") @DecimalMax("2.0") float motionIntensity
) {
    public VideoGenerateRequest {
        if (duration == 0) duration = 5;
        if (aspectRatio == null) aspectRatio = "16:9";
        if (fps == 0) fps = 24;
        if (quality == null) quality = "high";
        if (model == null) model = "kling-v1-5";
        if (style == null) style = "none";
        if (cfgScale == 0) cfgScale = 7.5f;
        if (motionIntensity == 0) motionIntensity = 1.0f;
    }
}
