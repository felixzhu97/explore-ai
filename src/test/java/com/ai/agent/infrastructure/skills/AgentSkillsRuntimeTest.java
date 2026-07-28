package com.ai.agent.infrastructure.skills;
import com.ai.agent.infrastructure.config.AgentProperties;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
class AgentSkillsRuntimeTest {
    @Test void should_stayInactive_when_disabledByDefault() { var r = runtime(false, List.of()); assertThat(r.enabled()).isFalse(); assertThat(r.augmentSystemPrompt("base")).isEqualTo("base"); }
    @Test void should_augmentPromptAndRegisterTool_when_enabledWithValidSkill() { var r = runtime(true, List.of("brief-style")); assertThat(r.enabled()).isTrue(); assertThat(r.augmentSystemPrompt("base")).contains("brief-style"); assertThat(r.skillToolCallback()).isPresent(); }
    @Test void should_failSoft_when_enabledWithInvalidSkillId() { var r = runtime(true, List.of("missing-skill")); assertThat(r.enabled()).isFalse(); }
    private static AgentSkillsRuntime runtime(boolean enabled, List<String> ids) { AgentProperties p = new AgentProperties(); p.getSkills().setEnabled(enabled); p.getSkills().setIds(ids); return new AgentSkillsRuntime(p, new AgentSkillLoader(p)); }
}
