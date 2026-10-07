package com.ai.image.domain.model;

import com.ai.common.exception.DomainException;

public record ImagePrompt(String value) {

  private static final int MAX_LENGTH = 4_000;

  public ImagePrompt {
    if (value == null || value.isBlank()) {
      throw DomainException.invalid("INVALID_IMAGE_PROMPT", "Image prompt must not be blank");
    }
    value = value.trim();
    if (value.length() > MAX_LENGTH) {
      throw DomainException.invalid(
          "INVALID_IMAGE_PROMPT", "Image prompt exceeds maximum length of " + MAX_LENGTH);
    }
  }

  /** Creates a prompt. */
  public static ImagePrompt createPrompt(String prompt) {
    return new ImagePrompt(prompt);
  }
}
