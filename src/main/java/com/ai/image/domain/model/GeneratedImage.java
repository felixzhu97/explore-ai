package com.ai.image.domain.model;

/** Documentation. */
public record GeneratedImage(String url, String base64, String model, String prompt) {
  /** Documentation. */
  public static GeneratedImage fromUrl(String url, String model, String prompt) {
    return new GeneratedImage(url, null, model, prompt);
  }

  /** Documentation. */
  public static GeneratedImage fromBase64(String base64, String model, String prompt) {
    return new GeneratedImage(null, base64, model, prompt);
  }

  /** Documentation. */
  public static GeneratedImage empty() {
    return new GeneratedImage(null, null, null, null);
  }

  /** Documentation. */
  public boolean hasUrl() {
    return url != null && !url.isBlank();
  }

  /** Documentation. */
  public boolean hasBase64() {
    return base64 != null && !base64.isBlank();
  }

  public boolean isAvailable() {
    return hasUrl() || hasBase64();
  }
}
