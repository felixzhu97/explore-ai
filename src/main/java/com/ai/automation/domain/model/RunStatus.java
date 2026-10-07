package com.ai.automation.domain.model;

/** Outcome of an automation run: succeeded, failed, or skipped (e.g. quota exceeded). */
public enum RunStatus {
  SUCCESS,
  FAILED,
  SKIPPED;

  /** Parses a run status, ignoring case. */
  public static RunStatus parseStatus(String text) {
    return RunStatus.valueOf(text.trim().toUpperCase());
  }
}
