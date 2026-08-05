package com.ai.skill.domain.exception;

public class SkillNotFoundException extends RuntimeException {

    private final String skillId;

    public SkillNotFoundException(String skillId) {
        super("Skill not found: " + skillId);
        this.skillId = skillId;
    }

    public String getSkillId() {
        return skillId;
    }
}
