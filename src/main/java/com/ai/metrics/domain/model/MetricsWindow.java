package com.ai.metrics.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import lombok.Value;

/** Metrics time range ending now, such as {@code 7d} or {@code 30d}. */
@Value
public class MetricsWindow {
  String range;
  Duration span;

  public static final String DEFAULT_RANGE = "7d";

  /** Parses a range, defaulting to {@value #DEFAULT_RANGE}; rejects unsupported ranges. */
  public static MetricsWindow parseWindow(String text) {
    String normalized =
        text == null || text.isBlank() ? DEFAULT_RANGE : text.trim().toLowerCase(Locale.ROOT);
    return switch (normalized) {
      case "7d" -> new MetricsWindow(normalized, Duration.ofDays(7));
      case "30d" -> new MetricsWindow(normalized, Duration.ofDays(30));
      default -> throw new IllegalArgumentException("Unsupported range: " + text);
    };
  }

  /** Returns the start of the window that ends at {@code now}. */
  public Instant from(Instant now) {
    return now.minus(span);
  }
}
