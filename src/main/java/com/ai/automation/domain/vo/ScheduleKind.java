package com.ai.automation.domain.vo;

public enum ScheduleKind {
    CRON,
    ONCE;

    public String value() {
        return name();
    }

    public static ScheduleKind from(String raw) {
        if (raw == null || raw.isBlank()) {
            return CRON;
        }
        return ScheduleKind.valueOf(raw.trim().toUpperCase());
    }
}
