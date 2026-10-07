package com.ai.skill.domain.model;

import com.ai.common.domain.model.AbstractUuidId;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Strongly-typed ID for {@link com.ai.skill.domain.model.Skill}. */
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public final class SkillId extends AbstractUuidId {

  public SkillId(String value) {
    super(value);
  }

  /** Wraps an existing id. */
  public static SkillId of(String value) {
    return new SkillId(value);
  }

  /** Creates a new random id. */
  public static SkillId generate() {
    return new SkillId(generateUuidString());
  }
}
