package com.ai.skill.domain.exception;

/** Thrown when an owner already has a skill with the requested name. */
public class SkillNameConflictException extends RuntimeException {

  private final String name;

  public SkillNameConflictException(String name) {
    super("Skill name already exists: " + name);
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
