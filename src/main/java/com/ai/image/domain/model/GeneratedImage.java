package com.ai.image.domain.model;

public record GeneratedImage(String url, String base64, String model, String prompt) {

    public static GeneratedImage fromUrl(String url, String model, String prompt) {
        return new GeneratedImage(url, null, model, prompt);
    }

    public static GeneratedImage fromBase64(String base64, String model, String prompt) {
        return new GeneratedImage(null, base64, model, prompt);
    }

    public static GeneratedImage empty() {
        return new GeneratedImage(null, null, null, null);
    }

    public boolean hasUrl() {
        return url != null && !url.isBlank();
    }

    public boolean hasBase64() {
        return base64 != null && !base64.isBlank();
    }

    public boolean isAvailable() {
        return hasUrl() || hasBase64();
    }
}
