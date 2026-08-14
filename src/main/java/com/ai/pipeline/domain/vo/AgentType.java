package com.ai.pipeline.domain.vo;

import java.util.Locale;
import java.util.Objects;

/** Identifier for a registered pipeline worker (supervisor or specialized worker). */
public record AgentType(String value) {
  /** Documentation. */
  public AgentType {
    Objects.requireNonNull(value, "agent type must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException("agent type must not be blank");
    }
    value = value.trim().toLowerCase(Locale.ROOT);
  }

  /** Documentation. */
  public static AgentType of(String value) {
    return new AgentType(value);
  }

  /** Documentation. */
  public static AgentType supervisor() {
    return new AgentType("supervisor");
  }

  public boolean isSupervisor() {
    return "supervisor".equals(value);
  }
}
