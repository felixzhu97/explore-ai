package com.ai.pipeline.domain.exception;

public class WorkflowTemplateNotFoundException extends RuntimeException {

    public WorkflowTemplateNotFoundException(String id) {
        super("Workflow template not found: " + id);
    }
}
