package com.ai.automation.domain.model;

/** Action an automation schedule performs when it fires. */
public enum AutomationActionType {
  RUN_PIPELINE_TEMPLATE;

  public String value() {
    return name();
  }

  /** Parses an action type, ignoring case. */
  public static AutomationActionType from(String raw) {
    return AutomationActionType.valueOf(raw.trim().toUpperCase());
  }
}
