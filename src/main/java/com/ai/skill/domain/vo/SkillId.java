package com.ai.skill.domain.vo;

import com.ai.base.domain.vo.AbstractUuidId;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Strongly-typed ID for {@link com.ai.skill.domain.model.Skill}. */
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public final class SkillId extends AbstractUuidId {

  /** Documentation. */
  public SkillId(String value) {
    super(value);
  }

  /** Documentation. */
  public static SkillId of(String value) {
    return new SkillId(value);
  }

  /** Documentation. */
  public static SkillId generate() {
    return new SkillId(newUuidString());
  }
}
