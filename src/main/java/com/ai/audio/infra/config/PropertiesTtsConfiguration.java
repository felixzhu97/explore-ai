package com.ai.audio.infra.config;

import com.ai.audio.domain.repository.TtsConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Exposes {@code TtsProperties} to the audio domain as a {@code TtsConfiguration}. */
@Component
@RequiredArgsConstructor
public class PropertiesTtsConfiguration implements TtsConfiguration {

  private final TtsProperties ttsProperties;

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
