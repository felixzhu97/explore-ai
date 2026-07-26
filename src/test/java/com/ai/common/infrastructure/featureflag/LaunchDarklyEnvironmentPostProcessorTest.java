package com.ai.common.infrastructure.featureflag;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LaunchDarklyEnvironmentPostProcessor")
class LaunchDarklyEnvironmentPostProcessorTest {

    private final LaunchDarklyEnvironmentPostProcessor processor = new LaunchDarklyEnvironmentPostProcessor();

    @Test
    @DisplayName("should_load_fallback_flags_when_sdk_key_missing")
    void should_load_fallback_flags_when_sdk_key_missing() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("launchdarkly.enabled", "true");
        environment.setProperty("launchdarkly.fallback.module-vision", "true");
        environment.setProperty("launchdarkly.fallback.module-mcp", "false");

        processor.postProcessEnvironment(environment, new SpringApplication(Object.class));

        MapPropertySource bootstrap = firstMapSource(environment);
        assertThat(bootstrap.getProperty("launchdarkly.bootstrap.module-vision")).isEqualTo(true);
        assertThat(bootstrap.getProperty("launchdarkly.bootstrap.module-mcp")).isEqualTo(false);
    }

    @Test
    @DisplayName("should_use_fallback_when_launchdarkly_disabled")
    void should_use_fallback_when_launchdarkly_disabled() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("launchdarkly.enabled", "false");
        environment.setProperty("launchdarkly.sdk-key", "sdk-test");
        environment.setProperty("launchdarkly.fallback.module-eval", "true");

        processor.postProcessEnvironment(environment, new SpringApplication(Object.class));

        MapPropertySource bootstrap = firstMapSource(environment);
        assertThat(bootstrap.getProperty("launchdarkly.bootstrap.module-eval")).isEqualTo(true);
    }

    @Test
    @DisplayName("should_default_fallback_flags_to_false")
    void should_default_fallback_flags_to_false() {
        MockEnvironment environment = new MockEnvironment();

        processor.postProcessEnvironment(environment, new SpringApplication(Object.class));

        MapPropertySource bootstrap = firstMapSource(environment);
        assertThat(bootstrap.getProperty("launchdarkly.bootstrap.module-agents")).isEqualTo(false);
    }

    private static MapPropertySource firstMapSource(MockEnvironment environment) {
        PropertySource<?> source = environment.getPropertySources().iterator().next();
        assertThat(source).isInstanceOf(MapPropertySource.class);
        return (MapPropertySource) source;
    }
}
