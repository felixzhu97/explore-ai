package com.ai.media.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ImageGenerationRequest(
        @NotBlank(message = "Prompt is required")
        String prompt,

        String negativePrompt,

        @Min(value = 256, message = "Width must be at least 256")
        @Max(value = 1024, message = "Width must not exceed 1024")
        Integer width,

        @Min(value = 256, message = "Height must be at least 256")
        @Max(value = 1024, message = "Height must not exceed 1024")
        Integer height,

        @Min(value = 1, message = "Steps must be at least 1")
        @Max(value = 100, message = "Steps must not exceed 100")
        Integer steps,

        @Min(value = 1, message = "CFG scale must be at least 1")
        @Max(value = 20, message = "CFG scale must not exceed 20")
        Float cfgScale,

        @Min(value = 0, message = "Seed must be non-negative")
        Long seed,

        String model,

        String lora
) {
    public ImageGenerationRequest {
        if (negativePrompt == null) {
            negativePrompt = "blurry, ugly, distorted, low quality, watermark, text, signature";
        }
        if (width == null) width = 512;
        if (height == null) height = 512;
        if (steps == null) steps = 25;
        if (cfgScale == null) cfgScale = 7.5f;
    }
}
