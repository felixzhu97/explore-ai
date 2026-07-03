package com.ai.eval.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for chat evaluation.
 */
public record EvaluationRequest(
    @NotBlank String userMessage,
    @NotBlank String assistantResponse
) {}
