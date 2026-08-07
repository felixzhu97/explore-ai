package com.ai.automation.domain.exception;

public class AutomationLimitExceededException extends RuntimeException {

    public AutomationLimitExceededException(String message) {
        super(message);
    }
}
