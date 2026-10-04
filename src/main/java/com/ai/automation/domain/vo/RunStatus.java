package com.ai.automation.domain.vo;

/** Outcome of an automation run: succeeded, failed, or skipped (e.g. quota exceeded). */
public enum RunStatus {
  SUCCESS,
  FAILED,
  SKIPPED;

  public String value() {
    return name();
  }

  public static RunStatus from(String raw) {
    return RunStatus.valueOf(raw.trim().toUpperCase());
  }
}
