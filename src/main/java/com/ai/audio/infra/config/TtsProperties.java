package com.ai.audio.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/** Documentation. */
@ConfigurationProperties(prefix = "app.ai.tts")
public class TtsProperties {

  private boolean enabled = true;

  /** media-gen (explore-ml Qwen3-TTS) or openai (Spring AI). */
  private String provider = "media-gen";

  private String model = "gpt-4o-mini-tts";
  private String voice = "alloy";
  private String apiKey = "";
  private String baseUrl = "https://api.openai.com/v1";
  private String mediaGenBaseUrl = "http://localhost:8003";

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

  public String getVoice() {
    return voice;
  }

  public void setVoice(String voice) {
    this.voice = voice;
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

  public String getMediaGenBaseUrl() {
    return mediaGenBaseUrl;
  }

  public void setMediaGenBaseUrl(String mediaGenBaseUrl) {
    this.mediaGenBaseUrl = mediaGenBaseUrl;
  }

  /** True when TTS is enabled and the active provider has required settings. */
  public boolean isConfigured() {
    if (!enabled) {
      return false;
    }
    if ("media-gen".equalsIgnoreCase(provider)) {
      return StringUtils.hasText(mediaGenBaseUrl);
    }
    return apiKey != null && !apiKey.isBlank();
  }
}
