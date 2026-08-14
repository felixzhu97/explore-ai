package com.ai.audio.domain.repository;

/** Documentation. */
public interface TtsConfiguration {
  /** Documentation. */
  boolean isEnabled();

  /** Documentation. */
  boolean isConfigured();

  /** Documentation. */
  String getDefaultVoice();
}
