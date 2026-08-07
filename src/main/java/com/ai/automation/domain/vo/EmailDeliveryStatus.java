package com.ai.automation.domain.vo;

public enum EmailDeliveryStatus {
    PENDING,
    SENT,
    SKIPPED,
    FAILED;

    public String value() {
        return name();
    }

    public static EmailDeliveryStatus from(String raw) {
        return EmailDeliveryStatus.valueOf(raw.trim().toUpperCase());
    }
}
