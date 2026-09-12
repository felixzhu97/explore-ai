package com.ai.audio.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/** Documentation. */
@ConfigurationProperties(prefix = "app.ai.tts")
public class TtsProperties {

  private boolean enabled = true;

  /** speech (explore-ml Qwen3-TTS) or openai (Spring AI). */
  private String provider = "speech";

  private String model = "gpt-4o-mini-tts";
  private String voice = "alloy";
  private String apiKey = "";
  private String baseUrl = "https://api.openai.com/v1";
  private String speechBaseUrl = "http://localhost:8004";

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

  public String getSpeechBaseUrl() {
    return speechBaseUrl;
  }

  public void setSpeechBaseUrl(String speechBaseUrl) {
    this.speechBaseUrl = speechBaseUrl;
  }

  /** True when TTS is enabled and the active provider has required settings. */
  public boolean isConfigured() {
    if (!enabled) {
      return false;
    }
    if ("speech".equalsIgnoreCase(provider)) {
      return StringUtils.hasText(speechBaseUrl);
    }
    return apiKey != null && !apiKey.isBlank();
  }
}
