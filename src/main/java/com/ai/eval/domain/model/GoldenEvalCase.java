package com.ai.eval.domain.model;

import com.ai.eval.domain.vo.GoldenEvalDomain;
import java.util.List;

/** One OpenAI Evals-style golden case (input + ideal + metadata). */
public record GoldenEvalCase(
    String id,
    GoldenEvalDomain domain,
    String userText,
    List<String> ideal,
    boolean toolsEnabled,
    List<String> contexts,
    List<String> documentIds,
    List<String> fixtureKeys) {
  /** Documentation. */
  public GoldenEvalCase {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("id is required");
    }
    if (userText == null || userText.isBlank()) {
      throw new IllegalArgumentException("input is required");
    }
    if (ideal == null || ideal.isEmpty()) {
      throw new IllegalArgumentException("ideal is required");
    }
    domain = domain == null ? GoldenEvalDomain.CHAT : domain;
    ideal = List.copyOf(ideal);
    contexts = contexts == null ? List.of() : List.copyOf(contexts);
    documentIds = documentIds == null ? List.of() : List.copyOf(documentIds);
    fixtureKeys = fixtureKeys == null ? List.of() : List.copyOf(fixtureKeys);
  }
}
