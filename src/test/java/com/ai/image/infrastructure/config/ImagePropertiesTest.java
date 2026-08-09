package com.ai.image.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ImageProperties")
class ImagePropertiesTest {

    @Test
    @DisplayName("should expose defaults and configuration rules")
    void shouldExposeDefaultsAndConfigurationRules() {
        ImageProperties properties = new ImageProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.isOllamaProvider()).isTrue();
        assertThat(properties.isConfigured()).isTrue();

        properties.setProvider(ImageProperties.PROVIDER_OPENAI);
        properties.setApiKey("");
        assertThat(properties.isOpenAiProvider()).isTrue();
        assertThat(properties.isConfigured()).isFalse();

        properties.setApiKey("sk-test");
        assertThat(properties.isConfigured()).isTrue();
    }
}
