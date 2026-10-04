package com.ai.audio.infra.config;

import com.ai.audio.domain.repository.TtsConfiguration;
import org.springframework.stereotype.Component;

/** Exposes {@code TtsProperties} to the audio domain as a {@code TtsConfiguration}. */
@Component
public class TtsConfigurationAdapter implements TtsConfiguration {

  private final TtsProperties ttsProperties;

  public TtsConfigurationAdapter(TtsProperties ttsProperties) {
    this.ttsProperties = ttsProperties;
  }

  @Override
  public boolean isEnabled() {
    return ttsProperties.isEnabled();
  }

  @Override
  public boolean isConfigured() {
    return ttsProperties.isConfigured();
  }

  @Override
  public String getDefaultVoice() {
    return ttsProperties.getVoice();
  }
}
