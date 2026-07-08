package com.ai.eval.web.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * Request DTO for chat evaluation.
 */
public record EvaluationRequest(
    @NotBlank String userMessage,
    @NotBlank String assistantResponse,
    List<String> referenceDocuments
) {
    public EvaluationRequest {
        referenceDocuments = referenceDocuments == null ? List.of() : List.copyOf(referenceDocuments);
    }
}
