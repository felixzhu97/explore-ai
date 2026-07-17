package com.ai.common.infrastructure.featureflag;

import com.ai.common.config.LaunchDarklyProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InMemoryFeatureFlagRepository")
class InMemoryFeatureFlagRepositoryTest {

    @Test
    @DisplayName("should return configured fallback before caller default")
    void should_returnConfiguredFallback_when_flagKeyPresent() {
        LaunchDarklyProperties properties = new LaunchDarklyProperties();
        properties.setFallback(Map.of(
                "module-vision", false,
                "module-mcp", true
        ));
        InMemoryFeatureFlagRepository repository = new InMemoryFeatureFlagRepository(properties);

        assertThat(repository.isEnabled("module-vision", true)).isFalse();
        assertThat(repository.isEnabled("module-mcp", false)).isTrue();
    }

    @Test
    @DisplayName("should return caller default when fallback is missing")
    void should_returnCallerDefault_when_fallbackMissing() {
        LaunchDarklyProperties properties = new LaunchDarklyProperties();
        InMemoryFeatureFlagRepository repository = new InMemoryFeatureFlagRepository(properties);

        assertThat(repository.isEnabled("module-eval", true)).isTrue();
        assertThat(repository.isEnabled("module-audio-asr", false)).isFalse();
    }
}
