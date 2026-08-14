package com.ai.skill.domain.exception;

/** Documentation. */
public class SkillNotFoundException extends RuntimeException {

  private final String skillId;

  /** Documentation. */
  public SkillNotFoundException(String skillId) {
    super("Skill not found: " + skillId);
    this.skillId = skillId;
  }

  public String getSkillId() {
    return skillId;
  }
}
