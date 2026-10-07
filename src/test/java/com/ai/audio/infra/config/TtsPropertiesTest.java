package com.ai.audio.infra.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TtsProperties")
class TtsPropertiesTest {

  @Test
  @DisplayName("should report not configured when disabled")
  void shouldReportNotConfiguredWhenDisabled() {
    TtsProperties properties = new TtsProperties();
    properties.setEnabled(false);
    properties.setApiKey("sk-test");

    assertThat(properties.isConfigured()).isFalse();
  }

  @Test
  @DisplayName("should report not configured when speech base URL is blank")
  void shouldReportNotConfiguredWhenSpeechBaseUrlIsBlank() {
    TtsProperties properties = new TtsProperties();
    properties.setSpeechBaseUrl(" ");

    assertThat(properties.isConfigured()).isFalse();
  }
}
