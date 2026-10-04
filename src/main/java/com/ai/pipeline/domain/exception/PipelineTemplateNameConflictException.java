package com.ai.pipeline.domain.exception;

/** Thrown when a client already has a pipeline template with the requested name. */
public class PipelineTemplateNameConflictException extends RuntimeException {
  public PipelineTemplateNameConflictException(String name) {
    super("Pipeline template name already exists: " + name);
  }
}
