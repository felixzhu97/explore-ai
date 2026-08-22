package com.ai.pipeline.domain.vo;

import com.ai.base.domain.vo.AbstractUuidId;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Strongly-typed ID for {@link com.ai.pipeline.domain.model.SavedAgentDefinition}. */
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public final class SavedAgentId extends AbstractUuidId {

  /** Documentation. */
  public SavedAgentId(String value) {
    super(value);
  }

  /** Documentation. */
  public static SavedAgentId of(String value) {
    return new SavedAgentId(value);
  }

  /** Documentation. */
  public static SavedAgentId generate() {
    return new SavedAgentId(newUuidString());
  }
}
