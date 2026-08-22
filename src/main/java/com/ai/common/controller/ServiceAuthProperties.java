package com.ai.common.controller;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Documentation. */
@ConfigurationProperties(prefix = "app.service-auth")
public class ServiceAuthProperties {

  /**
   * Shared secret for trusted BFF / service callers ({@code X-Service-Key}). When blank,
   * service-key client identity is disabled and cookie flow is used.
   */
  private String apiKey = "";

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public boolean isEnabled() {
    return apiKey != null && !apiKey.isBlank();
  }
}
