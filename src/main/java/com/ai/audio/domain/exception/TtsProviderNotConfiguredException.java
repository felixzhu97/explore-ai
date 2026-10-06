package com.ai.audio.domain.exception;

/** Thrown when text-to-speech is disabled or its provider lacks required settings. */
public class TtsProviderNotConfiguredException extends RuntimeException {
  public TtsProviderNotConfiguredException(String message) {
    super(message);
  }

  /** Creates the error for disabled text-to-speech. */
  public static TtsProviderNotConfiguredException disabled() {
    return new TtsProviderNotConfiguredException("Text-to-speech is disabled");
  }

  /** Creates the error for a missing text-to-speech API key. */
  public static TtsProviderNotConfiguredException apiKeyMissing() {
    return new TtsProviderNotConfiguredException(
        "TTS provider not configured. Set OPENAI_API_KEY or TTS_API_KEY");
  }
}
