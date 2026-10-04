package com.ai.automation.domain.vo;

/** Action an automation schedule performs when it fires. */
public enum AutomationActionType {
  RUN_SAVED_WORKFLOW;

  public String value() {
    return name();
  }

  public static AutomationActionType from(String raw) {
    return AutomationActionType.valueOf(raw.trim().toUpperCase());
  }
}
