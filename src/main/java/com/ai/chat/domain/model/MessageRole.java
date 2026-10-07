package com.ai.chat.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Locale;

/** Who wrote a chat message: {@code user} or {@code assistant} on the wire. */
public enum MessageRole {
  USER("user"),
  ASSISTANT("assistant");

  private final String value;

  MessageRole(String value) {
    this.value = value;
  }

  /** Returns the lowercase wire value. */
  @JsonValue
  public String getValue() {
    return value;
  }

  /** Parses a wire role case-insensitively; rejects unknown roles. */
  @JsonCreator
  public static MessageRole parseRole(String text) {
    String normalized = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    return Arrays.stream(values())
        .filter(role -> role.value.equals(normalized))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unsupported chat role: " + text));
  }
}
