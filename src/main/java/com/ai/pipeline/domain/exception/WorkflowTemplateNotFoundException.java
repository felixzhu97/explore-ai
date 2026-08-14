package com.ai.pipeline.domain.exception;

/** Documentation. */
public class WorkflowTemplateNotFoundException extends RuntimeException {
  /** Documentation. */
  public WorkflowTemplateNotFoundException(String id) {
    super("Workflow template not found: " + id);
  }
}
