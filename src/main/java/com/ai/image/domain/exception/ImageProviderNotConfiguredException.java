package com.ai.image.domain.exception;

public class ImageProviderNotConfiguredException extends RuntimeException {

    public ImageProviderNotConfiguredException(String message) {
        super(message);
    }

    public static ImageProviderNotConfiguredException ollamaModelMissing() {
        return new ImageProviderNotConfiguredException(
                "Image provider not configured. Run: ollama pull x/flux2-klein");
    }

    public static ImageProviderNotConfiguredException openAiKeyMissing() {
        return new ImageProviderNotConfiguredException(
                "Image provider not configured. Set OPENAI_API_KEY and app.ai.image.provider=openai");
    }

    public static ImageProviderNotConfiguredException disabled() {
        return new ImageProviderNotConfiguredException("Image generation is disabled");
    }
}
