package com.ai.pipeline.domain.exception;

public class WorkflowTemplateNameConflictException extends RuntimeException {

    public WorkflowTemplateNameConflictException(String name) {
        super("Workflow template name already exists: " + name);
    }
}
