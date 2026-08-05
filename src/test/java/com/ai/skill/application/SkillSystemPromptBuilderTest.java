package com.ai.skill.application;

import com.ai.skill.domain.model.Skill;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SkillSystemPromptBuilder")
class SkillSystemPromptBuilderTest {

    @Test
    @DisplayName("should_buildPrompt_when_skillsProvided")
    void should_buildPrompt_when_skillsProvided() {
        Skill skill = Skill.create(
                "client-1",
                "Brief Style",
                "Short answers only.",
                "Lead with the answer.",
                List.of());

        String prompt = SkillSystemPromptBuilder.build(List.of(skill));

        assertThat(prompt).contains("## Active Skills");
        assertThat(prompt).contains("follow the skills");
        assertThat(prompt).contains("### Brief Style");
        assertThat(prompt).contains("Short answers only.");
        assertThat(prompt).contains("Lead with the answer.");
    }

    @Test
    @DisplayName("should_returnNull_when_skillsEmpty")
    void should_returnNull_when_skillsEmpty() {
        assertThat(SkillSystemPromptBuilder.build(List.of())).isNull();
        assertThat(SkillSystemPromptBuilder.build(null)).isNull();
    }
}
