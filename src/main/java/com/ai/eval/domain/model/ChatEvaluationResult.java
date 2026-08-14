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
    boolean relevancyPass,
    Boolean factualityPass,
    List<String> evaluatorFeedback) {
  /** Documentation. */
  public static Builder builder() {
    return new Builder();
  }

  /** Documentation. */
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
    private boolean relevancyPass;
    private Boolean factualityPass;
    private List<String> evaluatorFeedback = List.of();

    /** Documentation. */
    public Builder coherenceScore(double score) {
      this.coherenceScore = Math.max(0, Math.min(1, score));
      return this;
    }

    /** Documentation. */
    public Builder relevanceScore(double score) {
      this.relevanceScore = Math.max(0, Math.min(1, score));
      return this;
    }

    /** Documentation. */
    public Builder helpfulnessScore(double score) {
      this.helpfulnessScore = Math.max(0, Math.min(1, score));
      return this;
    }

    /** Documentation. */
    public Builder factualityScore(Double score) {
      this.factualityScore = score == null ? null : Math.max(0, Math.min(1, score));
      return this;
    }

    /** Documentation. */
    public Builder factualityAvailable(boolean available) {
      this.factualityAvailable = available;
      return this;
    }

    /** Documentation. */
    public Builder overallScore(double score) {
      this.overallScore = Math.max(0, Math.min(1, score));
      return this;
    }

    /** Documentation. */
    public Builder hasSafetyIssues(boolean hasIssues) {
      this.hasSafetyIssues = hasIssues;
      return this;
    }

    /** Documentation. */
    public Builder safetyFlags(List<String> flags) {
      this.safetyFlags = List.copyOf(flags);
      return this;
    }

    /** Documentation. */
    public Builder suggestions(List<String> suggestions) {
      this.suggestions = List.copyOf(suggestions);
      return this;
    }

    /** Documentation. */
    public Builder relevancyPass(boolean pass) {
      this.relevancyPass = pass;
      return this;
    }

    /** Documentation. */
    public Builder factualityPass(Boolean pass) {
      this.factualityPass = pass;
      return this;
    }

    /** Documentation. */
    public Builder evaluatorFeedback(List<String> feedback) {
      this.evaluatorFeedback = feedback == null ? List.of() : List.copyOf(feedback);
      return this;
    }

    /** Documentation. */
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
          relevancyPass,
          factualityPass,
          evaluatorFeedback);
    }
  }
}
