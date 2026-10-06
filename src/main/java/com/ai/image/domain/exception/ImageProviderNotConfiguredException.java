package com.ai.image.domain.exception;

/** Thrown when image generation is disabled or its provider lacks a model or API key. */
public class ImageProviderNotConfiguredException extends RuntimeException {
  public ImageProviderNotConfiguredException(String message) {
    super(message);
  }

  /** Creates the error for a missing Ollama image model. */
  public static ImageProviderNotConfiguredException ollamaModelMissing() {
    return new ImageProviderNotConfiguredException(
        "Image provider not configured. Run: ollama pull x/flux2-klein");
  }

  /** Creates the error for a missing OpenAI key. */
  public static ImageProviderNotConfiguredException openAiKeyMissing() {
    return new ImageProviderNotConfiguredException(
        "Image provider not configured. Set OPENAI_API_KEY and app.ai.image.provider=openai");
  }

  /** Creates the error for turned-off image generation. */
  public static ImageProviderNotConfiguredException disabled() {
    return new ImageProviderNotConfiguredException("Image generation is disabled");
  }
}
