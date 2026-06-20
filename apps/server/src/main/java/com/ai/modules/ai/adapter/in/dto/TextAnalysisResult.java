package com.ai.adapter.in.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Structured output result for text analysis.
 * Demonstrates Spring AI 2.0 structured output with .entity() method.
 */
public record TextAnalysisResult(
    @JsonProperty("summary")
    String summary,
    
    @JsonProperty("sentiment")
    Sentiment sentiment,
    
    @JsonProperty("key_points")
    List<String> keyPoints,
    
    @JsonProperty("entities")
    List<String> entities,
    
    @JsonProperty("language")
    String language
) {
    public enum Sentiment {
        POSITIVE,
        NEUTRAL,
        NEGATIVE
    }
}
