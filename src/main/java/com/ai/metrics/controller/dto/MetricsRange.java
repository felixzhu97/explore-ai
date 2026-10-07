package com.ai.metrics.controller.dto;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

/** Metrics time window: the last 7 or 30 days. */
public enum MetricsRange {
  LAST_7_DAYS("7d"),
  LAST_30_DAYS("30d");

  private final String value;

  MetricsRange(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  /** Maps a normalized range such as {@code 7d}; rejects unknown ranges. */
  public static MetricsRange fromValue(String raw) {
    return Arrays.stream(values())
        .filter(range -> range.value.equals(raw))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unsupported range: " + raw));
  }
}
