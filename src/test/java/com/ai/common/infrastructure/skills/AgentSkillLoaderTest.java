package com.ai.common.infrastructure.skills;

import com.ai.common.domain.vo.AgentSkill;
import com.ai.common.infrastructure.config.AgentSkillsProperties;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import java.nio.charset.StandardCharsets;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class AgentSkillLoaderTest {
    @Test void should_returnEmpty_when_skillsDisabled() { assertThat(loader(false, List.of("brief-style")).loadEnabledSkills()).isEmpty(); }
    @Test void should_loadConfiguredSkill_when_enabled() {
        List<AgentSkill> skills = loader(true, List.of("brief-style")).loadEnabledSkills();
        assertThat(skills).hasSize(1);
        assertThat(skills.getFirst().name()).isEqualTo("brief-style");
    }
    @Test void should_skipInvalidSkill_when_frontmatterMissing() throws Exception {
        var resource = new ByteArrayResource("not frontmatter".getBytes(StandardCharsets.UTF_8)) { @Override public String getFilename() { return "SKILL.md"; } };
        assertThat(AgentSkillLoader.parseSkill(resource)).isEmpty();
    }
    @Test void should_ignoreUnlistedSkills_when_notInControlledList() { assertThat(loader(true, List.of("missing-skill")).loadEnabledSkills()).isEmpty(); }
    @Test void should_parseClasspathSkillResource() throws Exception {
        assertThat(AgentSkillLoader.parseSkill(new ClassPathResource("agent/skills/brief-style/SKILL.md"))).isPresent();
    }
    private static AgentSkillLoader loader(boolean enabled, List<String> ids) {
        AgentSkillsProperties p = new AgentSkillsProperties(); p.getSkills().setEnabled(enabled); p.getSkills().setIds(ids); return new AgentSkillLoader(p);
    }
}
