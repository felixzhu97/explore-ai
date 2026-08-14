package com.ai.audio.domain.exception;

/** Documentation. */
public class TtsProviderNotConfiguredException extends RuntimeException {
  /** Documentation. */
  public TtsProviderNotConfiguredException(String message) {
    super(message);
  }

  /** Documentation. */
  public static TtsProviderNotConfiguredException disabled() {
    return new TtsProviderNotConfiguredException("Text-to-speech is disabled");
  }

  /** Documentation. */
  public static TtsProviderNotConfiguredException apiKeyMissing() {
    return new TtsProviderNotConfiguredException(
        "TTS provider not configured. Set OPENAI_API_KEY or TTS_API_KEY");
  }
}
