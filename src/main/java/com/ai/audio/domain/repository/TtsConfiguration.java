package com.ai.audio.domain.repository;

/** Read-only view of TTS enablement, provider readiness, and the default voice. */
public interface TtsConfiguration {
  /** Tells whether text-to-speech is enabled. */
  boolean isEnabled();

  /** Tells whether the text-to-speech provider is configured. */
  boolean isConfigured();

  /** Returns the configured default voice. */
  String getDefaultVoice();
}
