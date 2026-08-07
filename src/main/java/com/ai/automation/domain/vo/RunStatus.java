package com.ai.automation.domain.vo;

public enum RunStatus {
    SUCCESS,
    FAILED,
    SKIPPED;

    public String value() {
        return name();
    }

    public static RunStatus from(String raw) {
        return RunStatus.valueOf(raw.trim().toUpperCase());
    }
}
