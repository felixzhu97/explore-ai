package com.ai.pipeline.domain.vo;

import com.ai.common.domain.vo.AbstractUuidId;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Strongly-typed ID for {@link com.ai.pipeline.domain.model.SavedAgent}. */
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public final class SavedAgentId extends AbstractUuidId {

  public SavedAgentId(String value) {
    super(value);
  }

  /** Wraps an existing id. */
  public static SavedAgentId of(String value) {
    return new SavedAgentId(value);
  }

  /** Creates a new random id. */
  public static SavedAgentId generate() {
    return new SavedAgentId(generateUuidString());
  }
}
