package com.ai.automation.domain.vo;

/** Documentation. */
public enum ScheduleKind {
  CRON,
  ONCE;

  /** Documentation. */
  public String value() {
    return name();
  }

  /** Documentation. */
  public static ScheduleKind from(String raw) {
    if (raw == null || raw.isBlank()) {
      return CRON;
    }
    return ScheduleKind.valueOf(raw.trim().toUpperCase());
  }
}
