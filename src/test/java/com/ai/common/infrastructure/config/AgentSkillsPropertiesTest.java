package com.ai.common.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentSkillsPropertiesTest {

    @Test
    void should_defaultSkillsOff_when_notConfigured() {
        assertThat(bind(Map.of()).getSkills().isEnabled()).isFalse();
    }

    @Test
    void should_bindSkillsConfig_when_propertiesProvided() {
        assertThat(bind(Map.of(
                "app.pipeline.skills.enabled", "true",
                "app.pipeline.skills.ids[0]", "brief-style"
        )).getSkills().getIds()).containsExactly("brief-style");
    }

    private static AgentSkillsProperties bind(Map<String, String> values) {
        return new Binder(new MapConfigurationPropertySource(values))
                .bind("app.pipeline", Bindable.of(AgentSkillsProperties.class))
                .orElseGet(AgentSkillsProperties::new);
    }
}
