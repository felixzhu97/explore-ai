package com.ai.audio.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.audio.tts.TextToSpeechModel;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TtsModelConfig")
class TtsModelConfigTest {

    @Test
    @DisplayName("should_create_text_to_speech_model_with_defaults")
    void should_create_text_to_speech_model_with_defaults() {
        TtsModelConfig config = new TtsModelConfig();
        TtsProperties properties = new TtsProperties();
        properties.setApiKey("test-key");
        properties.setBaseUrl("https://api.openai.com/v1/");

        TextToSpeechModel model = config.textToSpeechModel(properties);

        assertThat(model).isNotNull();
    }

    @Test
    @DisplayName("should_use_placeholder_key_when_api_key_blank")
    void should_use_placeholder_key_when_api_key_blank() {
        TtsModelConfig config = new TtsModelConfig();
        TtsProperties properties = new TtsProperties();
        properties.setApiKey("");

        TextToSpeechModel model = config.textToSpeechModel(properties);

        assertThat(model).isNotNull();
    }
}
