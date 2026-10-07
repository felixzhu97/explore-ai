package com.ai.automation.domain.model;

/** Action an automation schedule performs when it fires. */
public enum AutomationActionType {
  RUN_PIPELINE_TEMPLATE;

  /** Parses an action type, ignoring case. */
  public static AutomationActionType parseType(String text) {
    return AutomationActionType.valueOf(text.trim().toUpperCase());
  }
}
