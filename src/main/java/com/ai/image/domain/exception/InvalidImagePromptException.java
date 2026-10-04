package com.ai.image.domain.exception;

/** Thrown when an image prompt or its options (model, quality, size, count) are invalid. */
public class InvalidImagePromptException extends RuntimeException {
  public InvalidImagePromptException(String message) {
    super(message);
  }
}
