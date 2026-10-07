package com.ai.eval.controller.dto;

import com.ai.eval.domain.model.ChatEvaluationResult;
import java.util.List;

/** Response DTO for chat evaluation. */
public record EvaluationResponse(
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
  /** Maps a domain evaluation result to the response, rounding scores to two decimals. */
  public static EvaluationResponse from(ChatEvaluationResult result) {
    return new EvaluationResponse(
        round(result.getCoherenceScore()),
        round(result.getRelevanceScore()),
        round(result.getHelpfulnessScore()),
        result.isFactualityAvailable() ? round(result.getFactualityScore()) : null,
        result.isFactualityAvailable(),
        round(result.getOverallScore()),
        result.isHasSafetyIssues(),
        result.getSafetyFlags(),
        result.getSuggestions(),
        result.isRelevancyPassed(),
        result.getFactualityPassed(),
        result.getEvaluatorFeedback());
  }

  private static double round(double score) {
    return Math.round(score * 100.0) / 100.0;
  }
}
