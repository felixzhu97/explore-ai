package com.ai.image.infra.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration properties under {@code app.ai.image} selecting the Ollama or OpenAI provider. */
@ConfigurationProperties(prefix = "app.ai.image")
@Getter
@Setter
public class ImageProperties {

  public static final String PROVIDER_OLLAMA = "ollama";
  public static final String PROVIDER_OPENAI = "openai";

  private boolean enabled = true;
  private String provider = PROVIDER_OLLAMA;
  private String model = "x/flux2-klein";
  private String apiKey = "ollama";
  private String baseUrl = "http://localhost:11434/v1";

  public boolean isOllamaProvider() {
    return PROVIDER_OLLAMA.equalsIgnoreCase(provider);
  }

  public boolean isOpenAiProvider() {
    return PROVIDER_OPENAI.equalsIgnoreCase(provider);
  }

  /** Reports whether generation is enabled and has an OpenAI key or an Ollama base URL. */
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
