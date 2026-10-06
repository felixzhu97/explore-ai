package com.ai.skill.domain.exception;

/** Thrown when a skill id does not exist for the requesting owner. */
public class SkillNotFoundException extends RuntimeException {

  private final String skillId;

  public SkillNotFoundException(String skillId) {
    super("Skill not found: " + skillId);
    this.skillId = skillId;
  }

  /** Returns the skill id that was not found. */
  public String getSkillId() {
    return skillId;
  }
}
