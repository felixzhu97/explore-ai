package com.ai.pipeline.domain.model;

import java.util.Locale;
import java.util.Objects;

/** Identifier for a registered pipeline worker (supervisor or specialized worker). */
public record AgentType(String value) {
  public AgentType {
    Objects.requireNonNull(value, "agent type must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException("agent type must not be blank");
    }
    value = value.trim().toLowerCase(Locale.ROOT);
  }

  /** Creates an agent type. */
  public static AgentType of(String value) {
    return new AgentType(value);
  }

  /** Returns the supervisor type. */
  public static AgentType supervisor() {
    return new AgentType("supervisor");
  }

  /** Tells whether this is the supervisor type. */
  public boolean isSupervisor() {
    return "supervisor".equals(value);
  }
}
