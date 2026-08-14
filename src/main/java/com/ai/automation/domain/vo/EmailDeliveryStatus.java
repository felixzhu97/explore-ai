package com.ai.automation.domain.vo;

/** Documentation. */
public enum EmailDeliveryStatus {
  PENDING,
  SENT,
  SKIPPED,
  FAILED;

  /** Documentation. */
  public String value() {
    return name();
  }

  /** Documentation. */
  public static EmailDeliveryStatus from(String raw) {
    return EmailDeliveryStatus.valueOf(raw.trim().toUpperCase());
  }
}
