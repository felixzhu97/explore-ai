package com.ai.automation.domain.model;

/** Whether an automation schedule recurs on a cron expression or runs once at a set time. */
public enum ScheduleKind {
  CRON,
  ONCE;

  /** Parses a schedule kind case-insensitively, defaulting to {@code CRON} when blank. */
  public static ScheduleKind parseKind(String text) {
    if (text == null || text.isBlank()) {
      return CRON;
    }
    return ScheduleKind.valueOf(text.trim().toUpperCase());
  }
}
