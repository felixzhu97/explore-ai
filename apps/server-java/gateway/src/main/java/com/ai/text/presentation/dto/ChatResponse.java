package com.ai.text.presentation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Response DTO for chat completion.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatResponse(
        String text,
        String provider,
        String model,
        String sessionId,
        Map<String, Integer> usage,
        String finishReason
) {
    public static ChatResponse of(String text, String provider, String model, String sessionId) {
        return new ChatResponse(text, provider, model, sessionId, null, null);
    }

    public static ChatResponse of(String text, String provider, String model, String sessionId,
                                   Map<String, Integer> usage, String finishReason) {
        return new ChatResponse(text, provider, model, sessionId, usage, finishReason);
    }

    public ChatResponse withUsage(Map<String, Integer> usage) {
        return new ChatResponse(text, provider, model, sessionId, usage, finishReason);
    }

    public ChatResponse withFinishReason(String reason) {
        return new ChatResponse(text, provider, model, sessionId, usage, reason);
    }
}
