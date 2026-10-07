package com.ai.pipeline.domain.model;

import com.ai.common.domain.model.AbstractUuidId;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Strongly-typed ID for {@link com.ai.pipeline.domain.model.PipelineTemplate}. */
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public final class PipelineTemplateId extends AbstractUuidId {

  public PipelineTemplateId(String value) {
    super(value);
  }

  /** Wraps an existing id. */
  public static PipelineTemplateId of(String value) {
    return new PipelineTemplateId(value);
  }

  /** Creates a new random id. */
  public static PipelineTemplateId generate() {
    return new PipelineTemplateId(generateUuidString());
  }
}
