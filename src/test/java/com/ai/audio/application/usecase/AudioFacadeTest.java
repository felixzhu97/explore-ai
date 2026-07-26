package com.ai.audio.application.usecase;

import com.ai.audio.domain.exception.TtsProviderNotConfiguredException;
import com.ai.audio.domain.model.SynthesizedAudio;
import com.ai.audio.domain.repository.TextToSpeechRepository;
import com.ai.audio.domain.repository.TtsConfiguration;
import com.ai.audio.domain.vo.SpeechText;
import com.ai.audio.domain.vo.VoiceSelection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AudioFacade")
class AudioFacadeTest {

    @Mock
    private TextToSpeechRepository textToSpeechRepository;

    @Mock
    private TtsConfiguration ttsConfiguration;

    private AudioFacade facade;

    @BeforeEach
    void setUp() {
        facade = new AudioFacade(textToSpeechRepository, ttsConfiguration);
    }

    @Test
    @DisplayName("should_synthesize_speech_when_provider_configured")
    void should_synthesize_speech_when_provider_configured() {
        when(ttsConfiguration.isEnabled()).thenReturn(true);
        when(ttsConfiguration.isConfigured()).thenReturn(true);
        when(ttsConfiguration.getDefaultVoice()).thenReturn("alloy");
        when(textToSpeechRepository.synthesize(any(SpeechText.class), any(VoiceSelection.class), eq(1.0)))
                .thenReturn(SynthesizedAudio.create(new byte[] {1, 2, 3}));

        byte[] audio = facade.synthesize("hello", null, 1.0);

        assertThat(audio).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("should_resolve_legacy_voice_alias")
    void should_resolve_legacy_voice_alias() {
        when(ttsConfiguration.isEnabled()).thenReturn(true);
        when(ttsConfiguration.isConfigured()).thenReturn(true);
        when(textToSpeechRepository.synthesize(any(SpeechText.class), any(VoiceSelection.class), eq(null)))
                .thenReturn(SynthesizedAudio.create(new byte[] {9}));

        byte[] audio = facade.synthesize("hello", "zh-CN", null);

        assertThat(audio).containsExactly(9);
    }

    @Test
    @DisplayName("should_throw_when_tts_disabled")
    void should_throw_when_tts_disabled() {
        when(ttsConfiguration.isEnabled()).thenReturn(false);

        assertThatThrownBy(() -> facade.synthesize("hello", "alloy", null))
                .isInstanceOf(TtsProviderNotConfiguredException.class);
    }

    @Test
    @DisplayName("should_throw_when_api_key_missing")
    void should_throw_when_api_key_missing() {
        when(ttsConfiguration.isEnabled()).thenReturn(true);
        when(ttsConfiguration.isConfigured()).thenReturn(false);

        assertThatThrownBy(() -> facade.synthesize("hello", "alloy", null))
                .isInstanceOf(TtsProviderNotConfiguredException.class);
    }

    @Test
    @DisplayName("should_return_null_when_synthesized_audio_empty")
    void should_return_null_when_synthesized_audio_empty() {
        when(ttsConfiguration.isEnabled()).thenReturn(true);
        when(ttsConfiguration.isConfigured()).thenReturn(true);
        when(ttsConfiguration.getDefaultVoice()).thenReturn("alloy");
        when(textToSpeechRepository.synthesize(any(SpeechText.class), any(VoiceSelection.class), eq(null)))
                .thenReturn(SynthesizedAudio.empty());

        assertThat(facade.synthesize("hello", null, null)).isNull();
    }

    @Test
    @DisplayName("should_return_default_voice_catalog")
    void should_return_default_voice_catalog() {
        assertThat(facade.getAvailableVoices()).isNotEmpty();
        assertThat(facade.getAvailableTtsModels()).contains("gpt-4o-mini-tts");
    }
}
