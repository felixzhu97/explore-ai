package com.ai.analysis.controller.dto;

import com.ai.analysis.domain.model.Sentiment;
import com.ai.analysis.domain.model.TextAnalysis;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Structured text analysis response returned to API clients.
 *
 * @param summary analysis summary
 * @param sentiment sentiment label
 * @param keyPoints key points extracted from the text
 * @param entities named entities
 * @param language detected language
 */
public record TextAnalysisResult(
    @JsonProperty("summary") String summary,
    @JsonProperty("sentiment") SentimentDto sentiment,
    @JsonProperty("key_points") List<String> keyPoints,
    @JsonProperty("entities") List<String> entities,
    @JsonProperty("language") String language) {

  /** Sentiment label exposed in the analysis API response. */
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

  /** Maps a domain {@link TextAnalysis} to the API response DTO. */
  public static TextAnalysisResult fromDomain(TextAnalysis analysis) {
    return new TextAnalysisResult(
        analysis.summary(),
        SentimentDto.fromDomain(analysis.sentiment()),
        analysis.keyPoints(),
        analysis.entities(),
        analysis.language());
  }
}
