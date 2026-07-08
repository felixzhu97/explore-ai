package com.ai.analysis.web.dto;

import com.ai.analysis.domain.model.Sentiment;
import com.ai.analysis.domain.model.TextAnalysis;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TextAnalysisResult(
        @JsonProperty("summary") String summary,
        @JsonProperty("sentiment") SentimentDto sentiment,
        @JsonProperty("key_points") List<String> keyPoints,
        @JsonProperty("entities") List<String> entities,
        @JsonProperty("language") String language) {

    public enum SentimentDto {
        POSITIVE,
        NEUTRAL,
        NEGATIVE;

        static SentimentDto fromDomain(Sentiment sentiment) {
            if (sentiment == null) {
                return SentimentDto.NEUTRAL;
            }
            return SentimentDto.valueOf(sentiment.name());
        }
    }

    public static TextAnalysisResult fromDomain(TextAnalysis analysis) {
        return new TextAnalysisResult(
                analysis.summary(),
                SentimentDto.fromDomain(analysis.sentiment()),
                analysis.keyPoints(),
                analysis.entities(),
                analysis.language());
    }
}
