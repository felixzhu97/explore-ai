package com.ai.audio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.ai.audio.domain.model.SynthesizedAudio;
import com.ai.audio.domain.repository.TextToSpeechGateway;
import com.ai.audio.domain.repository.TtsConfiguration;
import com.ai.audio.domain.vo.SpeechText;
import com.ai.audio.domain.vo.VoiceSelection;
import com.ai.common.exception.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AudioService")
class AudioServiceTest {

  @Mock private TextToSpeechGateway textToSpeechGateway;

  @Mock private TtsConfiguration ttsConfiguration;

  private AudioService service;

  @BeforeEach
  void setUp() {
    service = new AudioService(textToSpeechGateway, ttsConfiguration);
  }

  @Test
  @DisplayName("should synthesize speech when provider configured")
  void shouldSynthesizeSpeechWhenProviderConfigured() {
    when(ttsConfiguration.isEnabled()).thenReturn(true);
    when(ttsConfiguration.isConfigured()).thenReturn(true);
    when(ttsConfiguration.getDefaultVoice()).thenReturn("alloy");
    when(textToSpeechGateway.synthesize(any(SpeechText.class), any(VoiceSelection.class), eq(1.0)))
        .thenReturn(SynthesizedAudio.create(new byte[] {1, 2, 3}));

    byte[] audio = service.synthesize("hello", null, 1.0);

    assertThat(audio).containsExactly(1, 2, 3);
  }

  @Test
  @DisplayName("should reject retired locale voice name")
  void shouldRejectRetiredLocaleVoiceName() {
    when(ttsConfiguration.isEnabled()).thenReturn(true);
    when(ttsConfiguration.isConfigured()).thenReturn(true);

    assertThatThrownBy(() -> service.synthesize("hello", "zh-CN", null))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("code", "INVALID_SPEECH_TEXT")
        .hasMessageContaining("Unknown voice");
  }

  @Test
  @DisplayName("should throw when tts disabled")
  void shouldThrowWhenTtsDisabled() {
    when(ttsConfiguration.isEnabled()).thenReturn(false);

    assertThatThrownBy(() -> service.synthesize("hello", "alloy", null))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("code", "TTS_PROVIDER_NOT_CONFIGURED");
  }

  @Test
  @DisplayName("should throw when api key missing")
  void shouldThrowWhenApiKeyMissing() {
    when(ttsConfiguration.isEnabled()).thenReturn(true);
    when(ttsConfiguration.isConfigured()).thenReturn(false);

    assertThatThrownBy(() -> service.synthesize("hello", "alloy", null))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("code", "TTS_PROVIDER_NOT_CONFIGURED");
  }

  @Test
  @DisplayName("should return null when synthesized audio empty")
  void shouldReturnNullWhenSynthesizedAudioEmpty() {
    when(ttsConfiguration.isEnabled()).thenReturn(true);
    when(ttsConfiguration.isConfigured()).thenReturn(true);
    when(ttsConfiguration.getDefaultVoice()).thenReturn("alloy");
    when(textToSpeechGateway.synthesize(any(SpeechText.class), any(VoiceSelection.class), eq(null)))
        .thenReturn(SynthesizedAudio.empty());

    assertThat(service.synthesize("hello", null, null)).isNull();
  }

  @Test
  @DisplayName("should return default voice catalog")
  void shouldReturnDefaultVoiceCatalog() {
    assertThat(service.getAvailableVoices()).isNotEmpty();
    assertThat(service.getAvailableTtsModels()).contains("gpt-4o-mini-tts");
  }
}
