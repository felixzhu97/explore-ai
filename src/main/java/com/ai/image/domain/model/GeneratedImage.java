package com.ai.image.domain.model;

public record GeneratedImage(String url, String base64, String model, String prompt) {
  /** Creates an image that has a URL. */
  public static GeneratedImage fromUrl(String url, String model, String prompt) {
    return new GeneratedImage(url, null, model, prompt);
  }

  /** Creates an image that has Base64 data. */
  public static GeneratedImage fromBase64(String base64, String model, String prompt) {
    return new GeneratedImage(null, base64, model, prompt);
  }

  /** Creates an image with no data. */
  public static GeneratedImage empty() {
    return new GeneratedImage(null, null, null, null);
  }

  /** Tells whether the image has a URL. */
  public boolean hasUrl() {
    return url != null && !url.isBlank();
  }

  /** Tells whether the image has Base64 data. */
  public boolean hasBase64() {
    return base64 != null && !base64.isBlank();
  }

  /** Tells whether the image has a URL or data. */
  public boolean isAvailable() {
    return hasUrl() || hasBase64();
  }
}
