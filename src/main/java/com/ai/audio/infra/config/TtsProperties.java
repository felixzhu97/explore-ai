package com.ai.audio.infra.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/** Configuration properties under {@code app.ai.tts} for the speech or OpenAI TTS provider. */
@ConfigurationProperties(prefix = "app.ai.tts")
@Getter
@Setter
public class TtsProperties {

  private boolean enabled = true;

  /** speech (explore-ml Qwen3-TTS) or openai (Spring AI). */
  private String provider = "speech";

  private String model = "gpt-4o-mini-tts";
  private String voice = "alloy";
  private String apiKey = "";
  private String baseUrl = "https://api.openai.com/v1";
  private String speechBaseUrl = "http://localhost:8000";

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
