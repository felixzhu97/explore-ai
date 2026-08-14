package com.ai.automation.domain.exception;

/** Documentation. */
public class AutomationScheduleNotFoundException extends RuntimeException {
  /** Documentation. */
  public AutomationScheduleNotFoundException(String scheduleId) {
    super("Automation schedule not found: " + scheduleId);
  }
}
