package com.ai.skill.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.skill.domain.model.Skill;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SkillSystemPromptBuilder")
class SkillSystemPromptBuilderTest {

  @Test
  @DisplayName("should build prompt when skills provided")
  void shouldBuildPromptWhenSkillsProvided() {
    Skill skill =
        Skill.create(
            "client-1", "Brief Style", "Short answers only.", "Lead with the answer.", List.of());

    String prompt = SkillSystemPromptBuilder.build(List.of(skill));

    assertThat(prompt).contains("## Active Skills");
    assertThat(prompt).contains("follow the skills");
    assertThat(prompt).contains("### Brief Style");
    assertThat(prompt).contains("Short answers only.");
    assertThat(prompt).contains("Lead with the answer.");
  }

  @Test
  @DisplayName("should return null when skills empty")
  void shouldReturnNullWhenSkillsEmpty() {
    assertThat(SkillSystemPromptBuilder.build(List.of())).isNull();
    assertThat(SkillSystemPromptBuilder.build(null)).isNull();
  }
}
