package com.ai.ai.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for image generation.
 */
public record ImageGenerationRequest(
        @NotBlank(message = "Prompt is required")
        String prompt,

        String model,

        String quality,

        Integer width,

        Integer height,

        Integer n
) {
    public ImageGenerationRequest {
        if (n == null) n = 1;
        if (width == null) width = 1024;
        if (height == null) height = 1024;
    }
}
