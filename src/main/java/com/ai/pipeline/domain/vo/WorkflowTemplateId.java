package com.ai.pipeline.domain.vo;

import java.util.UUID;

public record WorkflowTemplateId(String value) {

    public WorkflowTemplateId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("WorkflowTemplateId cannot be null or blank");
        }
    }

    public static WorkflowTemplateId of(String value) {
        return new WorkflowTemplateId(value);
    }

    public static WorkflowTemplateId generate() {
        return new WorkflowTemplateId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return value;
    }
}
