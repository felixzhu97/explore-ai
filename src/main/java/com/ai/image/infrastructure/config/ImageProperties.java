package com.ai.image.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai.image")
public class ImageProperties {

    public static final String PROVIDER_OLLAMA = "ollama";
    public static final String PROVIDER_OPENAI = "openai";

    private boolean enabled = true;
    private String provider = PROVIDER_OLLAMA;
    private String model = "x/flux2-klein";
    private String apiKey = "ollama";
    private String baseUrl = "http://localhost:11434/v1";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public boolean isOllamaProvider() {
        return PROVIDER_OLLAMA.equalsIgnoreCase(provider);
    }

    public boolean isOpenAiProvider() {
        return PROVIDER_OPENAI.equalsIgnoreCase(provider);
    }

    public boolean isConfigured() {
        if (!enabled) {
            return false;
        }
        if (isOpenAiProvider()) {
            return apiKey != null && !apiKey.isBlank();
        }
        return baseUrl != null && !baseUrl.isBlank();
    }
}
