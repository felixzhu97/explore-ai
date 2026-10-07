package com.ai.skill.service;

import com.ai.skill.domain.model.Skill;
import java.util.List;

/** Renders active skills into the "Active Skills" section of the chat system prompt. */
public final class SkillSystemPromptBuilder {

  private static final String HEADER =
      """
            ## Active Skills
            Apply the following skill instructions on every reply.
            When they conflict with the user's preferred length, tone, or format, follow the skills
            unless the user explicitly asks to ignore them for this turn.
            """
          .stripTrailing();

  private SkillSystemPromptBuilder() {}

  /** Builds the skills prompt section, or returns {@code null} when there are no skills. */
  public static String build(List<Skill> skills) {
    if (skills == null || skills.isEmpty()) {
      return null;
    }
    StringBuilder builder = new StringBuilder(HEADER);
    for (Skill skill : skills) {
      builder.append('\n').append(skill.buildPromptSection());
    }
    return builder.toString();
  }
}
