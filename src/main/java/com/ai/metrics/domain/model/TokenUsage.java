package com.ai.metrics.domain.model;

import lombok.Value;

/** Prompt and completion token counts; either is null when the provider omits it. */
@Value
public class TokenUsage {
  Integer prompt;
  Integer completion;

  public static final TokenUsage UNKNOWN = new TokenUsage(null, null);

  public TokenUsage(Integer prompt, Integer completion) {
    if ((prompt != null && prompt < 0) || (completion != null && completion < 0)) {
      throw new IllegalArgumentException("token counts must not be negative");
    }
    this.prompt = prompt;
    this.completion = completion;
  }

  /** Returns the counted tokens, treating missing counts as zero. */
  public long calculateTotal() {
    return (prompt == null ? 0L : prompt) + (completion == null ? 0L : completion);
  }
}
