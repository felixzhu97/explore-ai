package com.ai.common.infrastructure.featureflag;

import com.ai.common.domain.vo.ModuleFlag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.PropertySource;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LaunchDarklyEnvironmentPostProcessor")
class LaunchDarklyEnvironmentPostProcessorTest {

    private final LaunchDarklyEnvironmentPostProcessor postProcessor =
            new LaunchDarklyEnvironmentPostProcessor();

    @Test
    @DisplayName("should bootstrap fallback module flags when SDK key is missing")
    void should_bootstrapFallbackModuleFlags_when_sdkKeyMissing() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("launchdarkly.fallback.module-vision", "true")
                .withProperty("launchdarkly.fallback.module-audio-asr", "true")
                .withProperty("launchdarkly.fallback.module-mcp", "false");

        postProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(firstPropertySource(environment).getName()).isEqualTo("launchdarklyBootstrap");
        assertThat(environment.getProperty(ModuleFlag.VISION.bootstrapProperty(), Boolean.class)).isTrue();
        assertThat(environment.getProperty(ModuleFlag.AUDIO_ASR.bootstrapProperty(), Boolean.class)).isTrue();
        assertThat(environment.getProperty(ModuleFlag.MCP.bootstrapProperty(), Boolean.class)).isFalse();
        assertThat(environment.getProperty(ModuleFlag.EVAL.bootstrapProperty(), Boolean.class)).isFalse();
    }

    @Test
    @DisplayName("should bootstrap fallback module flags when LaunchDarkly is disabled")
    void should_bootstrapFallbackModuleFlags_when_launchDarklyDisabled() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("launchdarkly.enabled", "false")
                .withProperty("launchdarkly.sdk-key", "sdk-test-value")
                .withProperty("launchdarkly.fallback.module-eval", "true");

        postProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(firstPropertySource(environment).getName()).isEqualTo("launchdarklyBootstrap");
        assertThat(environment.getProperty(ModuleFlag.EVAL.bootstrapProperty(), Boolean.class)).isTrue();
        assertThat(environment.getProperty(ModuleFlag.VISION.bootstrapProperty(), Boolean.class)).isFalse();
        assertThat(environment.getProperty(ModuleFlag.AUDIO_ASR.bootstrapProperty(), Boolean.class)).isFalse();
        assertThat(environment.getProperty(ModuleFlag.MCP.bootstrapProperty(), Boolean.class)).isFalse();
    }

    private PropertySource<?> firstPropertySource(MockEnvironment environment) {
        return environment.getPropertySources().iterator().next();
    }
}
