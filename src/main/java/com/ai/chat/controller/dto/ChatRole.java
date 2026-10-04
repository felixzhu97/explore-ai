package com.ai.chat.controller.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Locale;

/** Author of a chat message on the wire: {@code user} or {@code assistant}. */
public enum ChatRole {
  USER("user"),
  ASSISTANT("assistant");

  private final String value;

  ChatRole(String value) {
    this.value = value;
  }

  @JsonValue
  public String value() {
    return value;
  }

  /** Parses a wire role case-insensitively; rejects unknown roles. */
  @JsonCreator
  public static ChatRole from(String raw) {
    String normalized = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    return Arrays.stream(values())
        .filter(role -> role.value.equals(normalized))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unsupported chat role: " + raw));
  }
}
