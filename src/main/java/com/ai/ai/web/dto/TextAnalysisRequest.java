package com.ai.ai.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for text analysis endpoint.
 */
public record TextAnalysisRequest(
    @NotBlank(message = "Text is required")
    String text,
    
    String language
) {
}
