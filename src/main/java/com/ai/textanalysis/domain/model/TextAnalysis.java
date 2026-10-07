package com.ai.textanalysis.domain.model;

import java.util.List;
import java.util.Objects;
import lombok.Getter;

/** Structured analysis result: summary, sentiment, key points, named entities, and language. */
@Getter
public class TextAnalysis {

  private final String summary;
  private final Sentiment sentiment;
  private final List<String> keyPoints;
  private final List<String> entities;
  private final String language;

  private TextAnalysis(
      String summary,
      Sentiment sentiment,
      List<String> keyPoints,
      List<String> entities,
      String language) {
    this.summary = summary;
    this.sentiment = Objects.requireNonNull(sentiment, "sentiment cannot be null");
    this.keyPoints =
        keyPoints == null ? List.of() : keyPoints.stream().filter(Objects::nonNull).toList();
    this.entities =
        entities == null ? List.of() : entities.stream().filter(Objects::nonNull).toList();
    this.language = language;
  }

  /** Creates an analysis result. */
  public static TextAnalysis create(
      String summary,
      Sentiment sentiment,
      List<String> keyPoints,
      List<String> entities,
      String language) {
    return new TextAnalysis(summary, sentiment, keyPoints, entities, language);
  }
}
