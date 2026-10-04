package com.ai.account.controller.dto;

import com.fasterxml.jackson.annotation.JsonValue;

/** Billing plan shown for the account: {@code free} unless the configured plan is pro. */
public enum AccountPlan {
  FREE("free"),
  PRO("pro");

  private final String value;

  AccountPlan(String value) {
    this.value = value;
  }

  @JsonValue
  public String value() {
    return value;
  }

  /** Maps the configured billing plan, case-insensitively, falling back to {@code free}. */
  public static AccountPlan from(String configured) {
    return PRO.value.equalsIgnoreCase(configured) ? PRO : FREE;
  }
}
