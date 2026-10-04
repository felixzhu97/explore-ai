package com.ai.vision.domain.exception;

/** Thrown when an uploaded file cannot be decoded as a supported image. */
public class VisionInvalidFileException extends RuntimeException {
  public VisionInvalidFileException(String message) {
    super(message);
  }
}
