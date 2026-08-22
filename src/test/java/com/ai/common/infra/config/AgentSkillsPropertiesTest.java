package com.ai.common.infra.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class AgentSkillsPropertiesTest {

  @Test
  void shouldDefaultSkillsOffWhenNotConfigured() {
    assertThat(bind(Map.of()).getSkills().isEnabled()).isFalse();
  }

  @Test
  void shouldBindSkillsConfigWhenPropertiesProvided() {
    assertThat(
            bind(Map.of(
                    "app.pipeline.skills.enabled", "true",
                    "app.pipeline.skills.ids[0]", "brief-style"))
                .getSkills()
                .getIds())
        .containsExactly("brief-style");
  }

  private static AgentSkillsProperties bind(Map<String, String> values) {
    return new Binder(new MapConfigurationPropertySource(values))
        .bind("app.pipeline", Bindable.of(AgentSkillsProperties.class))
        .orElseGet(AgentSkillsProperties::new);
  }
}
