package com.ai.automation.domain.model;

/** Outcome of an automation run: succeeded, failed, or skipped (e.g. quota exceeded). */
public enum RunStatus {
  SUCCESS,
  FAILED,
  SKIPPED;

  /** Parses a run status, ignoring case. */
  public static RunStatus parseStatus(String raw) {
    return RunStatus.valueOf(raw.trim().toUpperCase());
  }
}
