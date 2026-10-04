package com.ai.audio.domain.repository;

/** Read-only view of TTS enablement, provider readiness, and the default voice. */
public interface TtsConfiguration {
  boolean isEnabled();

  boolean isConfigured();

  String getDefaultVoice();
}
