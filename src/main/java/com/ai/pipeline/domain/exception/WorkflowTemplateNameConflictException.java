package com.ai.pipeline.domain.exception;

/** Documentation. */
public class WorkflowTemplateNameConflictException extends RuntimeException {
  /** Documentation. */
  public WorkflowTemplateNameConflictException(String name) {
    super("Workflow template name already exists: " + name);
  }
}
