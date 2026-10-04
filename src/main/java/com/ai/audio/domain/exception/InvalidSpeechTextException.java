package com.ai.audio.domain.exception;

/** Thrown when speech text is blank or too long, or the voice or model is unknown. */
public class InvalidSpeechTextException extends RuntimeException {
  public InvalidSpeechTextException(String message) {
    super(message);
  }
}
