package com.ai.eval.domain.model;

import java.util.List;

/** Per-case outcome of a golden suite run. */
public record CaseEvalOutcome(
    String id,
    GoldenEvalCategory category,
    String userText,
    String answer,
    boolean passed,
    boolean relevancyPassed,
    Boolean factualityPassed,
    List<String> feedback,
    String generationError) {
  public CaseEvalOutcome {
    feedback = feedback == null ? List.of() : List.copyOf(feedback);
    answer = answer == null ? "" : answer;
  }
}
