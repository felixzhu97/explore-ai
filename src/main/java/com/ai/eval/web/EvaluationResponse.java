package com.ai.eval.web;

import com.ai.eval.domain.ChatEvaluationResult;

import java.util.List;

/**
 * Response DTO for chat evaluation.
 */
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
    boolean relevancyPass,
    Boolean factualityPass,
    List<String> evaluatorFeedback
) {

    public static EvaluationResponse from(ChatEvaluationResult result) {
        return new EvaluationResponse(
            round(result.coherenceScore()),
            round(result.relevanceScore()),
            round(result.helpfulnessScore()),
            result.factualityAvailable() ? round(result.factualityScore()) : null,
            result.factualityAvailable(),
            round(result.overallScore()),
            result.hasSafetyIssues(),
            result.safetyFlags(),
            result.suggestions(),
            result.relevancyPass(),
            result.factualityPass(),
            result.evaluatorFeedback()
        );
    }

    private static double round(double score) {
        return Math.round(score * 100.0) / 100.0;
    }
}
