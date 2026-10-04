package com.ai.automation.domain.exception;

/** Thrown when a client tries to create more automation schedules than allowed. */
public class AutomationLimitExceededException extends RuntimeException {
  public AutomationLimitExceededException(String message) {
    super(message);
  }
}
