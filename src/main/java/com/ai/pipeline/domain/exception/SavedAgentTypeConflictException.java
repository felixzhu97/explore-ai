package com.ai.pipeline.domain.exception;

/** Thrown when a client's agent library already contains the requested type key. */
public class SavedAgentTypeConflictException extends RuntimeException {
  public SavedAgentTypeConflictException(String typeKey) {
    super("Agent type key already exists: " + typeKey);
  }
}
