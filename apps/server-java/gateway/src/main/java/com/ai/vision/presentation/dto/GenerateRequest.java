package com.ai.vision.presentation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GenerateRequest(
    @Size(min = 1, max = 4000) String prompt,

    @Size(max = 2000) String negativePrompt,

    @Min(256) @Max(2048) int width,

    @Min(256) @Max(2048) int height,

    @Min(1) @Max(150) int steps,

    @DecimalMin("1.0") @DecimalMax("20.0") float guidanceScale,

    Integer seed,

    @Min(1) @Max(4) int numImages,

    String stylePreset
) {
    public GenerateRequest {
        if (width <= 0) width = 1024;
        if (height <= 0) height = 1024;
        if (steps <= 0) steps = 30;
        if (guidanceScale <= 0) guidanceScale = 7.5f;
        if (negativePrompt == null) negativePrompt = "blurry, ugly, distorted, low quality, watermark, text, signature";
        if (numImages <= 0) numImages = 1;
    }

    public static GenerateRequest defaultConfig(String prompt) {
        return new GenerateRequest(prompt,
            "blurry, ugly, distorted, low quality, watermark, text, signature",
            1024, 1024, 30, 7.5f, null, 1, null);
    }
}
