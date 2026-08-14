package com.ai.skill.domain.exception;

/** Documentation. */
public class SkillNameConflictException extends RuntimeException {

  private final String name;

  /** Documentation. */
  public SkillNameConflictException(String name) {
    super("Skill name already exists: " + name);
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
