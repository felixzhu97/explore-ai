package com.ai.eval.domain.model;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Chat evaluation result containing quality scores and safety analysis. Scores are clamped to the
 * 0–1 range and missing lists read as empty.
 */
@Builder
@Value
public class ChatEvaluationResult {
  double coherenceScore;
  double relevanceScore;
  double helpfulnessScore;
  Double factualityScore;
  boolean factualityAvailable;
  double overallScore;
  boolean hasSafetyIssues;
  List<String> safetyFlags;
  List<String> suggestions;
  boolean relevancyPassed;
  Boolean factualityPassed;
  List<String> evaluatorFeedback;

  public ChatEvaluationResult(
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
    coherenceScore = clampScore(coherenceScore);
    relevanceScore = clampScore(relevanceScore);
    helpfulnessScore = clampScore(helpfulnessScore);
    factualityScore = factualityScore == null ? null : clampScore(factualityScore);
    overallScore = clampScore(overallScore);
    safetyFlags = copyOf(safetyFlags);
    suggestions = copyOf(suggestions);
    evaluatorFeedback = copyOf(evaluatorFeedback);
    this.coherenceScore = coherenceScore;
    this.relevanceScore = relevanceScore;
    this.helpfulnessScore = helpfulnessScore;
    this.factualityScore = factualityScore;
    this.factualityAvailable = factualityAvailable;
    this.overallScore = overallScore;
    this.hasSafetyIssues = hasSafetyIssues;
    this.safetyFlags = safetyFlags;
    this.suggestions = suggestions;
    this.relevancyPassed = relevancyPassed;
    this.factualityPassed = factualityPassed;
    this.evaluatorFeedback = evaluatorFeedback;
  }

  private static double clampScore(double score) {
    return Math.max(0, Math.min(1, score));
  }

  private static List<String> copyOf(List<String> values) {
    return values == null ? List.of() : List.copyOf(values);
  }
}
