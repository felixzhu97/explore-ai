package com.ai.image.domain.model;

public record GeneratedImage(String url, String model, String prompt) {

    public static GeneratedImage create(String url, String model, String prompt) {
        return new GeneratedImage(url, model, prompt);
    }

    public static GeneratedImage empty() {
        return new GeneratedImage(null, null, null);
    }

    public boolean hasUrl() {
        return url != null && !url.isBlank();
    }

    public boolean isAvailable() {
        return hasUrl();
    }
}
