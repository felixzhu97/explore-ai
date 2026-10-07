package com.ai.metrics.domain.model;

import java.util.Locale;

/** Result of an AI invocation, persisted as {@code success} or {@code error}. */
public enum InvocationOutcome {
  SUCCESS("success"),
  ERROR("error");

  private final String value;

  InvocationOutcome(String value) {
    this.value = value;
  }

  public String value() {
    return value;
  }

  /** Parses a case-insensitive outcome value, rejecting blank or unknown input. */
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
