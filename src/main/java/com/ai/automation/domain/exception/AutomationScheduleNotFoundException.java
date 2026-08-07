package com.ai.automation.domain.exception;

public class AutomationScheduleNotFoundException extends RuntimeException {

    public AutomationScheduleNotFoundException(String scheduleId) {
        super("Automation schedule not found: " + scheduleId);
    }
}
