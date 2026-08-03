package com.ai.eval.domain.model;

import java.util.List;

/**
 * Pass/fail gate from Spring AI RelevancyEvaluator and FactCheckingEvaluator.
 */
public record OfficialGateResult(
        boolean relevancyPass,
        Boolean factualityPass,
        boolean factualityEvaluated,
        double relevanceScore,
        Double factualityScore,
        List<String> feedback,
        boolean passed
) {
    public OfficialGateResult {
        feedback = feedback == null ? List.of() : List.copyOf(feedback);
    }
}
