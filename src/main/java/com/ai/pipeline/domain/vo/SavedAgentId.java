package com.ai.pipeline.domain.vo;

import java.util.UUID;

/** Documentation. */
public record SavedAgentId(String value) {
  /** Documentation. */
  public SavedAgentId {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("SavedAgentId cannot be null or blank");
    }
  }

  /** Documentation. */
  public static SavedAgentId of(String value) {
    return new SavedAgentId(value);
  }

  /** Documentation. */
  public static SavedAgentId generate() {
    return new SavedAgentId(UUID.randomUUID().toString());
  }
}
