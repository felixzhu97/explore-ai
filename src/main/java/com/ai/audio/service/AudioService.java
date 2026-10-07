package com.ai.audio.service;

import com.ai.audio.domain.model.SynthesizedAudio;
import com.ai.audio.domain.repository.TextToSpeechGateway;
import com.ai.audio.domain.repository.TtsConfiguration;
import com.ai.audio.domain.vo.SpeechText;
import com.ai.audio.domain.vo.VoiceCatalog;
import com.ai.audio.domain.vo.VoiceInfo;
import com.ai.audio.domain.vo.VoiceSelection;
import com.ai.common.exception.DomainException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Entry point for text-to-speech synthesis and the catalog of available voices and models. */
@Service
@RequiredArgsConstructor
public class AudioService {

  private final TextToSpeechGateway textToSpeechGateway;
  private final TtsConfiguration ttsConfiguration;

  /** Lists the voices available for text-to-speech. */
  public List<VoiceInfo> getAvailableVoices() {
    return VoiceCatalog.defaults().voiceInfos();
  }

  /** Lists the text-to-speech models. */
  public List<String> getAvailableTtsModels() {
    return VoiceCatalog.defaults().models();
  }

  /** Synthesizes speech and returns the audio with its media type, possibly empty. */
  public SynthesizedAudio synthesizeAudio(String text, String voice, Double speed) {
    ensureProviderConfigured();
    VoiceSelection selection = VoiceSelection.of(resolveVoice(voice), null);
    return textToSpeechGateway.synthesize(SpeechText.of(text), selection, speed);
  }

  /** Synthesizes speech and returns the raw audio bytes, or {@code null} when nothing came back. */
  public byte[] synthesize(String text, String voice, Double speed) {
    ensureProviderConfigured();
    VoiceSelection selection = VoiceSelection.of(resolveVoice(voice), null);
    SynthesizedAudio audio = textToSpeechGateway.synthesize(SpeechText.of(text), selection, speed);
    return audio.isEmpty() ? null : audio.data();
  }

  private void ensureProviderConfigured() {
    if (!ttsConfiguration.isEnabled()) {
      throw DomainException.unavailable(
          "TTS_PROVIDER_NOT_CONFIGURED", "Text-to-speech is disabled");
    }
    if (!ttsConfiguration.isConfigured()) {
      throw DomainException.unavailable(
          "TTS_PROVIDER_NOT_CONFIGURED",
          "TTS provider not configured. Set OPENAI_API_KEY or TTS_API_KEY");
    }
  }

  private String resolveVoice(String voice) {
    if (voice == null || voice.isBlank()) {
      return ttsConfiguration.getDefaultVoice();
    }
    return voice.trim();
  }
}
