package com.ai.pipeline.domain.exception;

/** Documentation. */
public class SavedAgentTypeConflictException extends RuntimeException {
  /** Documentation. */
  public SavedAgentTypeConflictException(String typeKey) {
    super("Agent type key already exists: " + typeKey);
  }
}
