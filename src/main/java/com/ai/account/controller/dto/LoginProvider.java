package com.ai.account.controller.dto;

import com.fasterxml.jackson.annotation.JsonValue;

/** OAuth registration id a user can sign in with. */
public enum LoginProvider {
  GOOGLE("google"),
  GITHUB("github"),
  EXPLORE_IAM("explore-iam");

  private final String value;

  LoginProvider(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }
}
