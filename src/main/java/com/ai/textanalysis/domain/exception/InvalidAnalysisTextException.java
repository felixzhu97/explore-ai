package com.ai.textanalysis.domain.exception;

/** Thrown when analysis input text is blank or exceeds the maximum length. */
public class InvalidAnalysisTextException extends RuntimeException {
  public InvalidAnalysisTextException(String message) {
    super(message);
  }
}
