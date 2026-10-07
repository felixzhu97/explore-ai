package com.ai.account.controller.dto;

import com.ai.billing.domain.model.Plan;
import com.fasterxml.jackson.annotation.JsonValue;

/** Billing plan shown for the account. */
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

  /** Maps the active billing plan. */
  public static AccountPlan from(Plan plan) {
    return plan == Plan.PRO ? PRO : FREE;
  }
}
