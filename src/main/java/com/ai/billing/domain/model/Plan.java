package com.ai.billing.domain.model;

/** Billing tier that sets the daily request limit. */
public enum Plan {
  FREE("free"),
  PRO("pro");

  private final String value;

  Plan(String value) {
    this.value = value;
  }

  /** Returns the lowercase wire value, such as {@code free}. */
  public String value() {
    return value;
  }

  /** Parses a configured plan case-insensitively; anything other than pro is free. */
  public static Plan parse(String configured) {
    return configured != null && PRO.value.equalsIgnoreCase(configured.trim()) ? PRO : FREE;
  }
}
