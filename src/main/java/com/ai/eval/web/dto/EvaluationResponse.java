package com.ai.eval.web.dto;

import com.ai.eval.domain.model.ChatEvaluationResult;

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
    List<String> suggestions
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
            result.suggestions()
        );
    }

    private static double round(double score) {
        return Math.round(score * 100.0) / 100.0;
    }
}
