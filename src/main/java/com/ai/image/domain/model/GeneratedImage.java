package com.ai.image.domain.model;

import java.util.Objects;

public class GeneratedImage {

    private final String url;
    private final String model;
    private final String prompt;

    private GeneratedImage(String url, String model, String prompt) {
        this.url = url;
        this.model = model;
        this.prompt = prompt;
    }

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

    public String url() {
        return url;
    }

    public String model() {
        return model;
    }

    public String prompt() {
        return prompt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GeneratedImage that)) {
            return false;
        }
        return Objects.equals(url, that.url)
                && Objects.equals(model, that.model)
                && Objects.equals(prompt, that.prompt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, model, prompt);
    }
}
