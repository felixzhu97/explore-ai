package com.ai.eval.domain.model;

import java.util.List;
import lombok.Value;

/** One OpenAI Evals-style golden case (input + ideal + metadata). */
@Value
public class GoldenEvalCase {
  String id;
  GoldenEvalCategory category;
  String userText;
  List<String> ideal;
  boolean toolsEnabled;
  List<String> contexts;
  List<String> documentIds;
  List<String> fixtureKeys;

  public GoldenEvalCase(
      String id,
      GoldenEvalCategory category,
      String userText,
      List<String> ideal,
      boolean toolsEnabled,
      List<String> contexts,
      List<String> documentIds,
      List<String> fixtureKeys) {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("id is required");
    }
    if (userText == null || userText.isBlank()) {
      throw new IllegalArgumentException("input is required");
    }
    if (ideal == null || ideal.isEmpty()) {
      throw new IllegalArgumentException("ideal is required");
    }
    category = category == null ? GoldenEvalCategory.CHAT : category;
    ideal = List.copyOf(ideal);
    contexts = contexts == null ? List.of() : List.copyOf(contexts);
    documentIds = documentIds == null ? List.of() : List.copyOf(documentIds);
    fixtureKeys = fixtureKeys == null ? List.of() : List.copyOf(fixtureKeys);
    this.id = id;
    this.category = category;
    this.userText = userText;
    this.ideal = ideal;
    this.toolsEnabled = toolsEnabled;
    this.contexts = contexts;
    this.documentIds = documentIds;
    this.fixtureKeys = fixtureKeys;
  }
}
