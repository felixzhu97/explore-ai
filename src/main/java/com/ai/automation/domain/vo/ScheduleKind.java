package com.ai.automation.domain.vo;

/** Whether an automation schedule recurs on a cron expression or runs once at a set time. */
public enum ScheduleKind {
  CRON,
  ONCE;

  public String value() {
    return name();
  }

  /** Parses a schedule kind case-insensitively, defaulting to {@code CRON} when blank. */
  public static ScheduleKind from(String raw) {
    if (raw == null || raw.isBlank()) {
      return CRON;
    }
    return ScheduleKind.valueOf(raw.trim().toUpperCase());
  }
}
