package com.ai.text.presentation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * DTO representing an LLM provider.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProviderInfo(
        String name,
        String displayName,
        List<String> models,
        String status
) {
    public static ProviderInfo of(String name, String displayName, List<String> models) {
        return new ProviderInfo(name, displayName, models, "active");
    }

    public static ProviderInfo of(String name, String displayName, List<String> models, String status) {
        return new ProviderInfo(name, displayName, models, status);
    }

    public ProviderInfo withStatus(String status) {
        return new ProviderInfo(name, displayName, models, status);
    }
}
