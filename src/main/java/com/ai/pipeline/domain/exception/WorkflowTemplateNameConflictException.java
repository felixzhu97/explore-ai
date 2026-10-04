package com.ai.pipeline.domain.exception;

/** Thrown when a client already has a workflow template with the requested name. */
public class WorkflowTemplateNameConflictException extends RuntimeException {
  public WorkflowTemplateNameConflictException(String name) {
    super("Workflow template name already exists: " + name);
  }
}
