package com.ai.skill.domain.exception;

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
