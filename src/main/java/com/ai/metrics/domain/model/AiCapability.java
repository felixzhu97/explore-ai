package com.ai.metrics.domain.model;

import jakarta.persistence.EnumeratedValue;
import java.util.Locale;
import java.util.Optional;

/** Business capabilities that emit AI invocation events for metrics. */
public enum AiCapability {
  CHAT("chat"),
  RAG("rag"),
  AGENTS("agents"),
  TOOLS("tools"),
  VISION("vision"),
  WORKFLOW("workflow");

  @EnumeratedValue private final String value;

  AiCapability(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  /** Resolves a case-insensitive capability value, returning empty when blank or unknown. */
  public static Optional<AiCapability> findCapability(String text) {
    if (text == null || text.isBlank()) {
      return Optional.empty();
    }
    String normalized = text.trim().toLowerCase(Locale.ROOT);
    for (AiCapability capability : values()) {
      if (capability.value.equals(normalized)) {
        return Optional.of(capability);
      }
    }
    return Optional.empty();
  }

  /** Parses a capability value, rejecting unknown values. */
  public static AiCapability parseCapability(String text) {
    return findCapability(text)
        .orElseThrow(() -> new IllegalArgumentException("Unknown AI capability: " + text));
  }
}
