package com.ai.vision.domain.exception;

/** Thrown when a vision provider (caption, detect, or OCR) has no loaded model or backend. */
public class VisionProviderUnavailableException extends RuntimeException {

  private final String provider;

  public VisionProviderUnavailableException(String provider, String message) {
    super(message);
    this.provider = provider;
  }

  public String getProvider() {
    return provider;
  }
}
