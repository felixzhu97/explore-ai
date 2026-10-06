package com.ai.skill.domain.vo;

import com.ai.common.domain.vo.AbstractUuidId;
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

  public static SkillId of(String value) {
    return new SkillId(value);
  }

  public static SkillId generate() {
    return new SkillId(generateUuidString());
  }
}
