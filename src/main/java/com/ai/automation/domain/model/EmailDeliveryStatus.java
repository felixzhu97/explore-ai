package com.ai.automation.domain.model;

/** Delivery state of the result email sent for an automation run. */
public enum EmailDeliveryStatus {
  PENDING,
  SENT,
  SKIPPED,
  FAILED;

  /** Parses a delivery status, ignoring case. */
  public static EmailDeliveryStatus parseStatus(String raw) {
    return EmailDeliveryStatus.valueOf(raw.trim().toUpperCase());
  }
}
