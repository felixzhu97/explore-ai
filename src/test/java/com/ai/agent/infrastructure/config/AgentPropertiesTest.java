package com.ai.agent.infrastructure.config;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
class AgentPropertiesTest {
    @Test void should_defaultSkillsOff_when_notConfigured() { assertThat(bind(Map.of()).getSkills().isEnabled()).isFalse(); }
    @Test void should_bindSkillsConfig_when_propertiesProvided() { assertThat(bind(Map.of("app.agent.skills.enabled","true","app.agent.skills.ids[0]","brief-style")).getSkills().getIds()).containsExactly("brief-style"); }
    private static AgentProperties bind(Map<String,String> values) { return new Binder(new MapConfigurationPropertySource(values)).bind("app.agent", Bindable.of(AgentProperties.class)).orElseGet(AgentProperties::new); }
}
