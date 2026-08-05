package com.ai.skill.application;

import com.ai.skill.domain.model.Skill;

import java.util.List;

public final class SkillSystemPromptBuilder {

    private static final String HEADER = """
            ## Active Skills
            Apply the following skill instructions on every reply.
            When they conflict with the user's preferred length, tone, or format, follow the skills
            unless the user explicitly asks to ignore them for this turn.
            """.stripTrailing();

    private SkillSystemPromptBuilder() {
    }

    public static String build(List<Skill> skills) {
        if (skills == null || skills.isEmpty()) {
            return null;
        }
        StringBuilder builder = new StringBuilder(HEADER);
        for (Skill skill : skills) {
            builder.append("\n### ").append(skill.getName()).append('\n');
            if (skill.getDescription() != null && !skill.getDescription().isBlank()) {
                builder.append(skill.getDescription()).append('\n');
            }
            builder.append(skill.getInstructions());
        }
        return builder.toString();
    }
}
