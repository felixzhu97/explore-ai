package com.ai.metrics.domain.vo;

import java.util.Locale;

/** Documentation. */
public enum InvocationOutcome {
  SUCCESS("success"),
  ERROR("error");

  private final String value;

  InvocationOutcome(String value) {
    this.value = value;
  }

  /** Documentation. */
  public String value() {
    return value;
  }

  /** Documentation. */
  public static InvocationOutcome parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("outcome must not be blank");
    }
    String normalized = raw.trim().toLowerCase(Locale.ROOT);
    for (InvocationOutcome outcome : values()) {
      if (outcome.value.equals(normalized)) {
        return outcome;
      }
    }
    throw new IllegalArgumentException("Unknown outcome: " + raw);
  }
}
