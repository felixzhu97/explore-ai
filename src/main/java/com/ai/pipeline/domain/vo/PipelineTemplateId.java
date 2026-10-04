package com.ai.pipeline.domain.vo;

import com.ai.common.domain.vo.AbstractUuidId;
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

  public static PipelineTemplateId of(String value) {
    return new PipelineTemplateId(value);
  }

  public static PipelineTemplateId generate() {
    return new PipelineTemplateId(newUuidString());
  }
}
