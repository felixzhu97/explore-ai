package com.ai.audio.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.audio.tts.TextToSpeechModel;

@DisplayName("TtsModelConfig")
class TtsModelConfigTest {

  @Test
  @DisplayName("should create text to speech model with defaults")
  void shouldCreateTextToSpeechModelWithDefaults() {
    TtsModelConfig config = new TtsModelConfig();
    TtsProperties properties = new TtsProperties();
    properties.setApiKey("test-key");
    properties.setBaseUrl("https://api.openai.com/v1/");

    TextToSpeechModel model = config.textToSpeechModel(properties);

    assertThat(model).isNotNull();
  }

  @Test
  @DisplayName("should use placeholder key when api key blank")
  void shouldUsePlaceholderKeyWhenApiKeyBlank() {
    TtsModelConfig config = new TtsModelConfig();
    TtsProperties properties = new TtsProperties();
    properties.setApiKey("");

    TextToSpeechModel model = config.textToSpeechModel(properties);

    assertThat(model).isNotNull();
  }
}
