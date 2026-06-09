package com.ai.text.presentation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO representing an LLM model.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ModelInfo(
        String name,
        String provider
) {
    public static ModelInfo of(String name, String provider) {
        return new ModelInfo(name, provider);
    }
}
