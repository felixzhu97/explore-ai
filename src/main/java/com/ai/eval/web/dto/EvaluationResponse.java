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
    double factualityScore,
    double overallScore,
    boolean hasSafetyIssues,
    List<String> safetyFlags,
    List<String> suggestions
) {

    public static EvaluationResponse from(ChatEvaluationResult result) {
        return new EvaluationResponse(
            Math.round(result.coherenceScore() * 100.0) / 100.0,
            Math.round(result.relevanceScore() * 100.0) / 100.0,
            Math.round(result.helpfulnessScore() * 100.0) / 100.0,
            Math.round(result.factualityScore() * 100.0) / 100.0,
            Math.round(result.overallScore() * 100.0) / 100.0,
            result.hasSafetyIssues(),
            result.safetyFlags(),
            result.suggestions()
        );
    }
}
