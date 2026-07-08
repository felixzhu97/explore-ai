package com.ai.eval.domain.model;

/**
 * Structured evaluation response from LLM judge.
 */
public record LlmEvaluationResponse(
    double coherenceScore,
    double helpfulnessScore,
    boolean hasSafetyIssues,
    String safetyConcern,
    String suggestion
) {}
