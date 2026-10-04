package com.ai.audio.service;

import com.ai.audio.domain.exception.TtsProviderNotConfiguredException;
import com.ai.audio.domain.model.SynthesizedAudio;
import com.ai.audio.domain.repository.TextToSpeechGateway;
import com.ai.audio.domain.repository.TtsConfiguration;
import com.ai.audio.domain.vo.SpeechText;
import com.ai.audio.domain.vo.VoiceCatalog;
import com.ai.audio.domain.vo.VoiceInfo;
import com.ai.audio.domain.vo.VoiceSelection;
import com.ai.common.infra.logging.LogSanitizer;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Entry point for text-to-speech synthesis and the catalog of available voices and models. */
@Service
public class AudioService {

  private static final Logger log = LoggerFactory.getLogger(AudioService.class);

  private final TextToSpeechGateway textToSpeechGateway;
  private final TtsConfiguration ttsConfiguration;

  public AudioService(TextToSpeechGateway textToSpeechGateway, TtsConfiguration ttsConfiguration) {
    this.textToSpeechGateway = textToSpeechGateway;
    this.ttsConfiguration = ttsConfiguration;
  }

  /** Synthesizes speech and returns the raw audio bytes, or {@code null} when nothing came back. */
  public byte[] synthesize(String text, String voice, Double speed) {
    ensureProviderConfigured();
    log.info("AudioService.synthesize: {}", LogSanitizer.truncate(text));
    VoiceSelection selection = VoiceSelection.of(resolveVoice(voice), null);
    SynthesizedAudio audio = textToSpeechGateway.synthesize(SpeechText.of(text), selection, speed);
    return audio.isEmpty() ? null : audio.data();
  }

  /** Synthesizes speech and returns the audio with its media type, possibly empty. */
  public SynthesizedAudio synthesizeAudio(String text, String voice, Double speed) {
    ensureProviderConfigured();
    log.info("AudioService.synthesize: {}", LogSanitizer.truncate(text));
    VoiceSelection selection = VoiceSelection.of(resolveVoice(voice), null);
    return textToSpeechGateway.synthesize(SpeechText.of(text), selection, speed);
  }

  public List<VoiceInfo> getAvailableVoices() {
    return VoiceCatalog.defaults().voiceInfos();
  }

  public List<String> getAvailableTtsModels() {
    return VoiceCatalog.defaults().models();
  }

  private void ensureProviderConfigured() {
    if (!ttsConfiguration.isEnabled()) {
      throw TtsProviderNotConfiguredException.disabled();
    }
    if (!ttsConfiguration.isConfigured()) {
      throw TtsProviderNotConfiguredException.apiKeyMissing();
    }
  }

  private String resolveVoice(String voice) {
    if (voice == null || voice.isBlank()) {
      return ttsConfiguration.getDefaultVoice();
    }
    return voice.trim();
  }
}
