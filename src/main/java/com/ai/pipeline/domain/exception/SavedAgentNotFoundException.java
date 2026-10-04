package com.ai.pipeline.domain.exception;

/** Thrown when a saved agent id does not exist for the requesting client. */
public class SavedAgentNotFoundException extends RuntimeException {
  public SavedAgentNotFoundException(String id) {
    super("Saved agent not found: " + id);
  }
}
