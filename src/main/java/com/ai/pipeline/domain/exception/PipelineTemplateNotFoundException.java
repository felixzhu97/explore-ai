package com.ai.pipeline.domain.exception;

/** Thrown when a saved pipeline template id does not exist for the requesting client. */
public class PipelineTemplateNotFoundException extends RuntimeException {
  public PipelineTemplateNotFoundException(String id) {
    super("Pipeline template not found: " + id);
  }
}
