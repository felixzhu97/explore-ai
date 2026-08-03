package com.ai.eval.domain.model;

import com.ai.eval.domain.vo.GoldenEvalDomain;

import java.util.List;

/**
 * Per-case outcome of a golden suite run.
 */
public record CaseEvalOutcome(
        String id,
        GoldenEvalDomain domain,
        String userText,
        String answer,
        boolean passed,
        boolean relevancyPass,
        Boolean factualityPass,
        List<String> feedback,
        String generationError
) {
    public CaseEvalOutcome {
        feedback = feedback == null ? List.of() : List.copyOf(feedback);
        answer = answer == null ? "" : answer;
    }
}
