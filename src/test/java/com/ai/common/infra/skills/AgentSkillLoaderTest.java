package com.ai.common.infra.skills;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.common.domain.vo.AgentSkill;
import com.ai.common.infra.config.AgentSkillsProperties;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

@DisplayName("AgentSkillLoader")
class AgentSkillLoaderTest {
  @Test
  void shouldReturnEmptyWhenSkillsDisabled() {
    assertThat(loader(false, List.of("brief-style")).loadEnabledSkills()).isEmpty();
  }

  @Test
  void shouldLoadConfiguredSkillWhenEnabled() {
    List<AgentSkill> skills = loader(true, List.of("brief-style")).loadEnabledSkills();
    assertThat(skills).hasSize(1);
    assertThat(skills.getFirst().name()).isEqualTo("brief-style");
  }

  @Test
  void shouldSkipInvalidSkillWhenFrontmatterMissing() throws Exception {
    var resource =
        new ByteArrayResource("not frontmatter".getBytes(StandardCharsets.UTF_8)) {
          @Override
          public String getFilename() {
            return "SKILL.md";
          }
        };
    assertThat(AgentSkillLoader.parseSkill(resource)).isEmpty();
  }

  @Test
  void shouldIgnoreUnlistedSkillsWhenNotInControlledList() {
    assertThat(loader(true, List.of("missing-skill")).loadEnabledSkills()).isEmpty();
  }

  @Test
  void shouldParseClasspathSkillResource() throws Exception {
    assertThat(
            AgentSkillLoader.parseSkill(new ClassPathResource("agent/skills/brief-style/SKILL.md")))
        .isPresent();
  }

  private static AgentSkillLoader loader(boolean enabled, List<String> ids) {
    AgentSkillsProperties p = new AgentSkillsProperties();
    p.getSkills().setEnabled(enabled);
    p.getSkills().setIds(ids);
    return new AgentSkillLoader(p);
  }
}
