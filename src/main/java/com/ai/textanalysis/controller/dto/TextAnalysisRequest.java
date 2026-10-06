package com.ai.textanalysis.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request DTO for text analysis endpoint. */
public record TextAnalysisRequest(
    @NotBlank(message = "Text is required")
        @Size(max = 10_000, message = "Text cannot exceed 10000 characters")
        String text,
    @Size(max = 20) String language) {}
