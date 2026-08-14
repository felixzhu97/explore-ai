package com.ai.pipeline.domain.exception;

/** Documentation. */
public class SavedAgentNotFoundException extends RuntimeException {
  /** Documentation. */
  public SavedAgentNotFoundException(String id) {
    super("Saved agent not found: " + id);
  }
}
