package com.ai.audio.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TtsProperties")
class TtsPropertiesTest {

  @Test
  @DisplayName("should expose defaults and setters")
  void shouldExposeDefaultsAndSetters() {
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
  @DisplayName("should report not configured when disabled")
  void shouldReportNotConfiguredWhenDisabled() {
    TtsProperties properties = new TtsProperties();
    properties.setEnabled(false);
    properties.setApiKey("sk-test");

    assertThat(properties.isConfigured()).isFalse();
  }
}
