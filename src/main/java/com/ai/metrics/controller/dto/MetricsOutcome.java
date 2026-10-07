package com.ai.metrics.controller.dto;

import com.ai.metrics.domain.model.InvocationOutcome;
import com.fasterxml.jackson.annotation.JsonValue;

/** Invocation outcome on the metrics wire: {@code success} or {@code error}. */
public enum MetricsOutcome {
  SUCCESS,
  ERROR;

  @JsonValue
  public String value() {
    return InvocationOutcome.valueOf(name()).getValue();
  }

  /** Maps an outcome to its API value. */
  public static MetricsOutcome from(InvocationOutcome outcome) {
    return valueOf(outcome.name());
  }
}
