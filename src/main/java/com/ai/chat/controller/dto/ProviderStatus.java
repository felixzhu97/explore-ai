package com.ai.chat.controller.dto;

import com.fasterxml.jackson.annotation.JsonValue;

/** Whether a chat provider can serve requests right now. */
public enum ProviderStatus {
  AVAILABLE("available"),
  UNAVAILABLE("unavailable");

  private final String value;

  ProviderStatus(String value) {
    this.value = value;
  }

  @JsonValue
  public String value() {
    return value;
  }

  public static ProviderStatus of(boolean available) {
    return available ? AVAILABLE : UNAVAILABLE;
  }
}
