package com.ai.image.domain.model;

import com.ai.common.exception.DomainException;
import lombok.Value;

/** Prompt describing the image to generate, non-blank and capped in length. */
@Value
public class ImagePrompt {
  String value;

  private static final int MAX_LENGTH = 4_000;

  public ImagePrompt(String value) {
    if (value == null || value.isBlank()) {
      throw DomainException.createInvalidError(
          "INVALID_IMAGE_PROMPT", "Image prompt must not be blank");
    }
    value = value.trim();
    if (value.length() > MAX_LENGTH) {
      throw DomainException.createInvalidError(
          "INVALID_IMAGE_PROMPT", "Image prompt exceeds maximum length of " + MAX_LENGTH);
    }
    this.value = value;
  }

  /** Creates a prompt. */
  public static ImagePrompt createPrompt(String prompt) {
    return new ImagePrompt(prompt);
  }
}
