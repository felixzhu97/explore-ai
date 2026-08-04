package com.ai.audio.infrastructure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TtsProperties")
class TtsPropertiesTest {

    @Test
    @DisplayName("should_expose_defaults_and_setters")
    void should_expose_defaults_and_setters() {
        TtsProperties properties = new TtsProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getModel()).isEqualTo("gpt-4o-mini-tts");
        assertThat(properties.getVoice()).isEqualTo("alloy");
        assertThat(properties.getBaseUrl()).isEqualTo("https://api.openai.com/v1");
        assertThat(properties.isConfigured()).isFalse();

        properties.setEnabled(true);
        properties.setApiKey("sk-test");
        properties.setModel("tts-1");
        properties.setVoice("nova");
        properties.setBaseUrl("https://example.com/v1/");

        assertThat(properties.isConfigured()).isTrue();
        assertThat(properties.getModel()).isEqualTo("tts-1");
        assertThat(properties.getVoice()).isEqualTo("nova");
    }

    @Test
    @DisplayName("should_report_not_configured_when_disabled")
    void should_report_not_configured_when_disabled() {
        TtsProperties properties = new TtsProperties();
        properties.setEnabled(false);
        properties.setApiKey("sk-test");

        assertThat(properties.isConfigured()).isFalse();
    }
}
