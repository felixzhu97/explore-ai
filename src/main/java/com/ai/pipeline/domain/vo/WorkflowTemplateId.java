package com.ai.pipeline.domain.vo;

import com.ai.base.domain.vo.AbstractUuidId;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Strongly-typed ID for {@link com.ai.pipeline.domain.model.SavedWorkflowTemplate}. */
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public final class WorkflowTemplateId extends AbstractUuidId {

  /** Documentation. */
  public WorkflowTemplateId(String value) {
    super(value);
  }

  /** Documentation. */
  public static WorkflowTemplateId of(String value) {
    return new WorkflowTemplateId(value);
  }

  /** Documentation. */
  public static WorkflowTemplateId generate() {
    return new WorkflowTemplateId(newUuidString());
  }
}
