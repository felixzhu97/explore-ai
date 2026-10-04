package com.ai.account.controller.dto;

import com.fasterxml.jackson.annotation.JsonValue;

/** Whether the browser is a guest or linked to a signed-in account. */
public enum AccountMode {
  ANONYMOUS("anonymous"),
  AUTHENTICATED("authenticated");

  private final String value;

  AccountMode(String value) {
    this.value = value;
  }

  @JsonValue
  public String value() {
    return value;
  }
}
