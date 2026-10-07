package com.ai.metrics.domain.model;

import jakarta.persistence.EnumeratedValue;
import java.util.Locale;

/** Result of an AI invocation, persisted as {@code success} or {@code error}. */
public enum InvocationOutcome {
  SUCCESS("success"),
  ERROR("error");

  @EnumeratedValue private final String value;

  InvocationOutcome(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  /** Parses a case-insensitive outcome value, rejecting blank or unknown input. */
  public static InvocationOutcome parseOutcome(String text) {
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException("outcome must not be blank");
    }
    String normalized = text.trim().toLowerCase(Locale.ROOT);
    for (InvocationOutcome outcome : values()) {
      if (outcome.value.equals(normalized)) {
        return outcome;
      }
    }
    throw new IllegalArgumentException("Unknown outcome: " + text);
  }
}
