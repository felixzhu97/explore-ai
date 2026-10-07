package com.ai.eval.domain.model;

import java.util.List;
import lombok.Value;

/** Per-case outcome of a golden suite run. */
@Value
public class CaseEvalOutcome {
  String id;
  GoldenEvalCategory category;
  String userText;
  String answer;
  boolean passed;
  boolean relevancyPassed;
  Boolean factualityPassed;
  List<String> feedback;
  String generationError;

  public CaseEvalOutcome(
      String id,
      GoldenEvalCategory category,
      String userText,
      String answer,
      boolean passed,
      boolean relevancyPassed,
      Boolean factualityPassed,
      List<String> feedback,
      String generationError) {
    feedback = feedback == null ? List.of() : List.copyOf(feedback);
    answer = answer == null ? "" : answer;
    this.id = id;
    this.category = category;
    this.userText = userText;
    this.answer = answer;
    this.passed = passed;
    this.relevancyPassed = relevancyPassed;
    this.factualityPassed = factualityPassed;
    this.feedback = feedback;
    this.generationError = generationError;
  }
}
