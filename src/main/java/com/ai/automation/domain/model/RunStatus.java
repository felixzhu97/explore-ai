package com.ai.automation.domain.model;

/** Outcome of an automation run: succeeded, failed, or skipped (e.g. quota exceeded). */
public enum RunStatus {
  SUCCESS,
  FAILED,
  SKIPPED;

  public String value() {
    return name();
  }

  /** Parses a run status, ignoring case. */
  public static RunStatus from(String raw) {
    return RunStatus.valueOf(raw.trim().toUpperCase());
  }
}
