package com.ai.pipeline.domain.vo;

import java.util.UUID;

/** Documentation. */
public record WorkflowTemplateId(String value) {
  /** Documentation. */
  public WorkflowTemplateId {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("WorkflowTemplateId cannot be null or blank");
    }
  }

  /** Documentation. */
  public static WorkflowTemplateId of(String value) {
    return new WorkflowTemplateId(value);
  }

  /** Documentation. */
  public static WorkflowTemplateId generate() {
    return new WorkflowTemplateId(UUID.randomUUID().toString());
  }

  @Override
  public String toString() {
    return value;
  }
}
