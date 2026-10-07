package com.ai.common.infra.skills;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.common.domain.model.BundledSkill;
import com.ai.common.infra.config.AgentSkillsProperties;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

@DisplayName("BundledSkillLoader")
class BundledSkillLoaderTest {
  @Test
  void shouldReturnEmptyWhenSkillsDisabled() {
    assertThat(loader(false, List.of("brief-style")).loadEnabledSkills()).isEmpty();
  }

  @Test
  void shouldLoadConfiguredSkillWhenEnabled() {
    List<BundledSkill> skills = loader(true, List.of("brief-style")).loadEnabledSkills();
    assertThat(skills).hasSize(1);
    assertThat(skills.getFirst().getName()).isEqualTo("brief-style");
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
    assertThat(BundledSkillLoader.parseSkill(resource)).isEmpty();
  }

  @Test
  void shouldIgnoreUnlistedSkillsWhenNotInControlledList() {
    assertThat(loader(true, List.of("missing-skill")).loadEnabledSkills()).isEmpty();
  }

  @Test
  void shouldParseClasspathSkillResource() throws Exception {
    assertThat(
            BundledSkillLoader.parseSkill(
                new ClassPathResource("agent/skills/brief-style/SKILL.md")))
        .isPresent();
  }

  private static BundledSkillLoader loader(boolean enabled, List<String> ids) {
    AgentSkillsProperties p = new AgentSkillsProperties();
    p.setEnabled(enabled);
    p.setIds(ids);
    return new BundledSkillLoader(p);
  }
}
