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
  public static EvaluationResponse createResponse(ChatEvaluationResult result) {
    return new EvaluationResponse(
        roundScore(result.getCoherenceScore()),
        roundScore(result.getRelevanceScore()),
        roundScore(result.getHelpfulnessScore()),
        result.isFactualityAvailable() ? roundScore(result.getFactualityScore()) : null,
        result.isFactualityAvailable(),
        roundScore(result.getOverallScore()),
        result.isHasSafetyIssues(),
        result.getSafetyFlags(),
        result.getSuggestions(),
        result.isRelevancyPassed(),
        result.getFactualityPassed(),
        result.getEvaluatorFeedback());
  }

  private static double roundScore(double score) {
    return Math.round(score * 100.0) / 100.0;
  }
}
