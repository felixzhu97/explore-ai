package com.ai.vision.domain.exception;

/** Documentation. */
public class VisionProviderUnavailableException extends RuntimeException {

  private final String provider;

  /** Documentation. */
  public VisionProviderUnavailableException(String provider, String message) {
    super(message);
    this.provider = provider;
  }

  public String getProvider() {
    return provider;
  }
}
