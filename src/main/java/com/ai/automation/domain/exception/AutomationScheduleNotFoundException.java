package com.ai.automation.domain.exception;

/** Thrown when an automation schedule id does not exist for the requesting client. */
public class AutomationScheduleNotFoundException extends RuntimeException {
  public AutomationScheduleNotFoundException(String scheduleId) {
    super("Automation schedule not found: " + scheduleId);
  }
}
