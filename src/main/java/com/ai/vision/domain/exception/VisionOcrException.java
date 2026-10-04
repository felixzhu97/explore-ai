package com.ai.vision.domain.exception;

/** Thrown when the OCR engine fails while extracting text from an image. */
public class VisionOcrException extends RuntimeException {
  public VisionOcrException(String message, Throwable cause) {
    super(message, cause);
  }
}
