package com.ai.eval.domain.model;

import java.util.List;

/** Chat evaluation result containing quality scores and safety analysis. */
public record ChatEvaluationResult(
    double coherenceScore,
    double relevanceScore,
    double helpfulnessScore,
    Double factualityScore,
    boolean factualityAvailable,
    double overallScore,
    boolean hasSafetyIssues,
    List<String> safetyFlags,
    List<String> suggestions,
    boolean relevancyPassed,
    Boolean factualityPassed,
    List<String> evaluatorFeedback) {
  /** Creates a result builder. */
  public static Builder builder() {
    return new Builder();
  }

  /** Fluent builder that clamps scores to the 0–1 range and copies list fields. */
  public static class Builder {
    private double coherenceScore;
    private double relevanceScore;
    private double helpfulnessScore;
    private Double factualityScore;
    private boolean factualityAvailable;
    private double overallScore;
    private boolean hasSafetyIssues;
    private List<String> safetyFlags = List.of();
    private List<String> suggestions = List.of();
    private boolean relevancyPassed;
    private Boolean factualityPassed;
    private List<String> evaluatorFeedback = List.of();

    /** Sets the coherence score, clamped to 0–1. */
    public Builder coherenceScore(double score) {
      this.coherenceScore = Math.max(0, Math.min(1, score));
      return this;
    }

    /** Sets the relevance score, clamped to 0–1. */
    public Builder relevanceScore(double score) {
      this.relevanceScore = Math.max(0, Math.min(1, score));
      return this;
    }

    /** Sets the helpfulness score, clamped to 0–1. */
    public Builder helpfulnessScore(double score) {
      this.helpfulnessScore = Math.max(0, Math.min(1, score));
      return this;
    }

    /** Sets the factuality score, clamped to 0–1, or null. */
    public Builder factualityScore(Double score) {
      this.factualityScore = score == null ? null : Math.max(0, Math.min(1, score));
      return this;
    }

    /** Sets whether a factuality score is available. */
    public Builder factualityAvailable(boolean available) {
      this.factualityAvailable = available;
      return this;
    }

    /** Sets the overall score, clamped to 0–1. */
    public Builder overallScore(double score) {
      this.overallScore = Math.max(0, Math.min(1, score));
      return this;
    }

    /** Sets whether safety issues were found. */
    public Builder hasSafetyIssues(boolean hasIssues) {
      this.hasSafetyIssues = hasIssues;
      return this;
    }

    /** Sets the safety flags. */
    public Builder safetyFlags(List<String> flags) {
      this.safetyFlags = List.copyOf(flags);
      return this;
    }

    /** Sets the suggestions. */
    public Builder suggestions(List<String> suggestions) {
      this.suggestions = List.copyOf(suggestions);
      return this;
    }

    /** Sets whether the relevancy check passed. */
    public Builder relevancyPassed(boolean pass) {
      this.relevancyPassed = pass;
      return this;
    }

    /** Sets whether the factuality check passed, or null. */
    public Builder factualityPassed(Boolean pass) {
      this.factualityPassed = pass;
      return this;
    }

    /** Sets the evaluator feedback. */
    public Builder evaluatorFeedback(List<String> feedback) {
      this.evaluatorFeedback = feedback == null ? List.of() : List.copyOf(feedback);
      return this;
    }

    /** Creates the evaluation result from the collected values. */
    public ChatEvaluationResult build() {
      return new ChatEvaluationResult(
          coherenceScore,
          relevanceScore,
          helpfulnessScore,
          factualityScore,
          factualityAvailable,
          overallScore,
          hasSafetyIssues,
          safetyFlags,
          suggestions,
          relevancyPassed,
          factualityPassed,
          evaluatorFeedback);
    }
  }
}
