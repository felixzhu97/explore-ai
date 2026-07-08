package com.ai.analysis.domain.model;

import java.util.List;
import java.util.Objects;

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
        this.keyPoints = List.copyOf(keyPoints != null ? keyPoints : List.of());
        this.entities = List.copyOf(entities != null ? entities : List.of());
        this.language = language;
    }

    public static TextAnalysis create(
            String summary,
            Sentiment sentiment,
            List<String> keyPoints,
            List<String> entities,
            String language) {
        return new TextAnalysis(summary, sentiment, keyPoints, entities, language);
    }

    public boolean isPositive() {
        return sentiment.isPositive();
    }

    public boolean hasEntities() {
        return !entities.isEmpty();
    }

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

    public String summary() {
        return summary;
    }

    public Sentiment sentiment() {
        return sentiment;
    }

    public List<String> keyPoints() {
        return keyPoints;
    }

    public List<String> entities() {
        return entities;
    }

    public String language() {
        return language;
    }
}
