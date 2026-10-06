package com.ai.textanalysis.domain.model;

/** Overall sentiment of analyzed text. */
public enum Sentiment {
  POSITIVE,
  NEUTRAL,
  NEGATIVE;

  /** Parses a case-insensitive sentiment name, falling back to {@code NEUTRAL}. */
  public static Sentiment fromString(String value) {
    if (value == null || value.isBlank()) {
      return NEUTRAL;
    }
    try {
      return valueOf(value.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      return NEUTRAL;
    }
  }

  /** Tells whether the sentiment is negative. */
  public boolean isNegative() {
    return this == NEGATIVE;
  }

  /** Tells whether the sentiment is positive. */
  public boolean isPositive() {
    return this == POSITIVE;
  }
}
