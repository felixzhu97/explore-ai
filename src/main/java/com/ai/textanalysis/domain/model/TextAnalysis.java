package com.ai.textanalysis.domain.model;

import java.util.List;
import java.util.Objects;

/** Structured analysis result: summary, sentiment, key points, named entities, and language. */
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

  /** Tells whether the sentiment is positive. */
  public boolean isPositive() {
    return sentiment.isPositive();
  }

  /** Tells whether any entities were found. */
  public boolean hasEntities() {
    return !entities.isEmpty();
  }

  /** Returns a copy whose summary is cut to at most {@code maxWords} words. */
  public TextAnalysis truncateSummary(int maxWords) {
    if (summary == null || summary.isBlank() || maxWords <= 0) {
      return this;
    }
    String[] words = summary.trim().split("\\s+");
    if (words.length <= maxWords) {
      return this;
    }
    String truncated = String.join(" ", List.of(words).subList(0, maxWords));
    return create(truncated, sentiment, keyPoints, entities, language);
  }

  /** Returns the summary. */
  public String summary() {
    return summary;
  }

  /** Returns the sentiment. */
  public Sentiment sentiment() {
    return sentiment;
  }

  /** Returns the key points. */
  public List<String> keyPoints() {
    return keyPoints;
  }

  /** Returns the entities. */
  public List<String> entities() {
    return entities;
  }

  /** Returns the language. */
  public String language() {
    return language;
  }
}
