package com.ai.metrics.domain.model;

/** Prompt and completion token counts; either is null when the provider omits it. */
public record TokenUsage(Integer prompt, Integer completion) {

  public static final TokenUsage UNKNOWN = new TokenUsage(null, null);

  public TokenUsage {
    if ((prompt != null && prompt < 0) || (completion != null && completion < 0)) {
      throw new IllegalArgumentException("token counts must not be negative");
    }
  }

  /** Returns the counted tokens, treating missing counts as zero. */
  public long calculateTotal() {
    return (prompt == null ? 0L : prompt) + (completion == null ? 0L : completion);
  }
}
