package com.ai.pipeline.domain.model;

import java.util.Locale;
import java.util.Objects;
import lombok.Value;

/** Identifier for a registered pipeline worker (supervisor or specialized worker). */
@Value
public class AgentType {
  String value;

  public AgentType(String value) {
    Objects.requireNonNull(value, "agent type must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException("agent type must not be blank");
    }
    value = value.trim().toLowerCase(Locale.ROOT);
    this.value = value;
  }

  /** Creates an agent type. */
  public static AgentType createType(String value) {
    return new AgentType(value);
  }

  /** Returns the supervisor type. */
  public static AgentType createSupervisorType() {
    return new AgentType("supervisor");
  }

  /** Tells whether this is the supervisor type. */
  public boolean isSupervisor() {
    return "supervisor".equals(value);
  }
}
