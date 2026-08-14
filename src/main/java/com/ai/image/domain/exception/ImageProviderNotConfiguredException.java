package com.ai.image.domain.exception;

/** Documentation. */
public class ImageProviderNotConfiguredException extends RuntimeException {
  /** Documentation. */
  public ImageProviderNotConfiguredException(String message) {
    super(message);
  }

  /** Documentation. */
  public static ImageProviderNotConfiguredException ollamaModelMissing() {
    return new ImageProviderNotConfiguredException(
        "Image provider not configured. Run: ollama pull x/flux2-klein");
  }

  /** Documentation. */
  public static ImageProviderNotConfiguredException openAiKeyMissing() {
    return new ImageProviderNotConfiguredException(
        "Image provider not configured. Set OPENAI_API_KEY and app.ai.image.provider=openai");
  }

  /** Documentation. */
  public static ImageProviderNotConfiguredException disabled() {
    return new ImageProviderNotConfiguredException("Image generation is disabled");
  }
}
