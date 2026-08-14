package com.ai.automation.domain.vo;

/** Documentation. */
public enum RunStatus {
  SUCCESS,
  FAILED,
  SKIPPED;

  /** Documentation. */
  public String value() {
    return name();
  }

  /** Documentation. */
  public static RunStatus from(String raw) {
    return RunStatus.valueOf(raw.trim().toUpperCase());
  }
}
