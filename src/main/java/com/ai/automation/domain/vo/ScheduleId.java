package com.ai.automation.domain.vo;

import java.util.Objects;
import java.util.UUID;

/** Documentation. */
public record ScheduleId(String value) {
  /** Documentation. */
  public ScheduleId {
    Objects.requireNonNull(value, "ScheduleId cannot be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException("ScheduleId cannot be blank");
    }
  }

  /** Documentation. */
  public static ScheduleId generate() {
    return new ScheduleId(UUID.randomUUID().toString());
  }

  /** Documentation. */
  public static ScheduleId of(String value) {
    return new ScheduleId(value);
  }
}
