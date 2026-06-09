package com.ai.text.presentation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for chat completion.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatRequest(
        List<ChatMessage> messages,
        String sessionId,
        String systemPrompt,
        Double temperature,
        Integer maxTokens,
        String provider,
        String model
) {
    public ChatRequest {
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("Messages cannot be null or empty");
        }
        if (temperature != null && (temperature < 0 || temperature > 2)) {
            throw new IllegalArgumentException("Temperature must be between 0 and 2");
        }
        if (maxTokens != null && maxTokens < 1) {
            throw new IllegalArgumentException("Max tokens must be at least 1");
        }
        if (provider != null) {
            provider = provider.toLowerCase().trim();
        }
        if (model != null) {
            model = model.trim();
        }
    }

    public String effectiveProvider() {
        return provider != null ? provider : "openai";
    }

    public String effectiveModel() {
        return model != null ? model : "gpt-4o-mini";
    }

    public double effectiveTemperature() {
        return temperature != null ? temperature : 0.7;
    }

    public int effectiveMaxTokens() {
        return maxTokens != null ? maxTokens : 2000;
    }
}
