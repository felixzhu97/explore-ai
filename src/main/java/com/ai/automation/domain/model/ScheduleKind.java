package com.ai.automation.domain.model;

/** Whether an automation schedule recurs on a cron expression or runs once at a set time. */
public enum ScheduleKind {
  CRON,
  ONCE;

  /** Parses a schedule kind case-insensitively, defaulting to {@code CRON} when blank. */
  public static ScheduleKind parseKind(String raw) {
    if (raw == null || raw.isBlank()) {
      return CRON;
    }
    return ScheduleKind.valueOf(raw.trim().toUpperCase());
  }
}
