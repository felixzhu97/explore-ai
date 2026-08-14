package com.ai.automation.domain.vo;

/** Documentation. */
public enum AutomationActionType {
  RUN_SAVED_WORKFLOW;

  /** Documentation. */
  public String value() {
    return name();
  }

  /** Documentation. */
  public static AutomationActionType from(String raw) {
    return AutomationActionType.valueOf(raw.trim().toUpperCase());
  }
}
