package com.ai.skill.domain.vo;

import java.util.UUID;

public record SkillId(String value) {

    public SkillId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("SkillId cannot be null or blank");
        }
    }

    public static SkillId of(String value) {
        return new SkillId(value);
    }

    public static SkillId generate() {
        return new SkillId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return value;
    }
}
