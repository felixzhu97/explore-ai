package com.ai.common.infra.skills;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.common.infra.config.AgentSkillsProperties;
import java.util.List;
import org.junit.jupiter.api.Test;

class AgentSkillsRuntimeTest {
  @Test
  void shouldStayInactiveWhenDisabledByDefault() {
    var r = runtime(false, List.of());
    assertThat(r.enabled()).isFalse();
    assertThat(r.augmentSystemPrompt("base")).isEqualTo("base");
  }

  @Test
  void shouldAugmentPromptAndRegisterToolWhenEnabledWithValidSkill() {
    var r = runtime(true, List.of("brief-style"));
    assertThat(r.enabled()).isTrue();
    assertThat(r.augmentSystemPrompt("base")).contains("brief-style");
    assertThat(r.skillToolCallback()).isPresent();
  }

  @Test
  void shouldFailSoftWhenEnabledWithInvalidSkillId() {
    var r = runtime(true, List.of("missing-skill"));
    assertThat(r.enabled()).isFalse();
  }

  private static AgentSkillsRuntime runtime(boolean enabled, List<String> ids) {
    AgentSkillsProperties p = new AgentSkillsProperties();
    p.getSkills().setEnabled(enabled);
    p.getSkills().setIds(ids);
    return new AgentSkillsRuntime(p, new AgentSkillLoader(p));
  }
}
