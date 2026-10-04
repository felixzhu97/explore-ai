package com.ai.pipeline.domain.exception;

/** Thrown when a saved workflow template id does not exist for the requesting client. */
public class WorkflowTemplateNotFoundException extends RuntimeException {
  public WorkflowTemplateNotFoundException(String id) {
    super("Workflow template not found: " + id);
  }
}
