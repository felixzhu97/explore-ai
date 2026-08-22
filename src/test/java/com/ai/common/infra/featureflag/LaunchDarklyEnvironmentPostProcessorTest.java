package com.ai.common.infra.featureflag;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.mock.env.MockEnvironment;

@DisplayName("LaunchDarklyEnvironmentPostProcessor")
class LaunchDarklyEnvironmentPostProcessorTest {

  private final LaunchDarklyEnvironmentPostProcessor processor =
      new LaunchDarklyEnvironmentPostProcessor();

  @Test
  @DisplayName("should load fallback flags when sdk key missing")
  void shouldLoadFallbackFlagsWhenSdkKeyMissing() {
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
  @DisplayName("should use fallback when launchdarkly disabled")
  void shouldUseFallbackWhenLaunchdarklyDisabled() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("launchdarkly.enabled", "false");
    environment.setProperty("launchdarkly.sdk-key", "sdk-test");
    environment.setProperty("launchdarkly.fallback.module-eval", "true");

    processor.postProcessEnvironment(environment, new SpringApplication(Object.class));

    MapPropertySource bootstrap = firstMapSource(environment);
    assertThat(bootstrap.getProperty("launchdarkly.bootstrap.module-eval")).isEqualTo(true);
  }

  @Test
  @DisplayName("should default fallback flags to false")
  void shouldDefaultFallbackFlagsToFalse() {
    MockEnvironment environment = new MockEnvironment();

    processor.postProcessEnvironment(environment, new SpringApplication(Object.class));

    MapPropertySource bootstrap = firstMapSource(environment);
    assertThat(bootstrap.getProperty("launchdarkly.bootstrap.module-pipelines")).isEqualTo(false);
  }

  private static MapPropertySource firstMapSource(MockEnvironment environment) {
    PropertySource<?> source = environment.getPropertySources().iterator().next();
    assertThat(source).isInstanceOf(MapPropertySource.class);
    return (MapPropertySource) source;
  }
}
