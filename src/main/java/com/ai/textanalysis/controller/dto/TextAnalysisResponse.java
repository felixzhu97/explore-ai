package com.ai.textanalysis.controller.dto;

import com.ai.textanalysis.domain.model.Sentiment;
import com.ai.textanalysis.domain.model.TextAnalysis;
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
public record TextAnalysisResponse(
    String summary,
    SentimentLabel sentiment,
    List<String> keyPoints,
    List<String> entities,
    String language) {

  /** Sentiment label exposed in the analysis API response. */
  public enum SentimentLabel {
    POSITIVE,
    NEUTRAL,
    NEGATIVE;

    static SentimentLabel fromDomain(Sentiment sentiment) {
      if (sentiment == null) {
        return SentimentLabel.NEUTRAL;
      }
      return SentimentLabel.valueOf(sentiment.name());
    }
  }

  /** Maps a domain {@link TextAnalysis} to the API response DTO. */
  public static TextAnalysisResponse fromDomain(TextAnalysis analysis) {
    return new TextAnalysisResponse(
        analysis.summary(),
        SentimentLabel.fromDomain(analysis.sentiment()),
        analysis.keyPoints(),
        analysis.entities(),
        analysis.language());
  }
}
