package com.ai.common.infra.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Service-to-service auth settings holding the shared {@code X-Service-Key} secret. */
@ConfigurationProperties(prefix = "app.service-auth")
@Getter
@Setter
public class ServiceAuthProperties {

  /**
   * Shared secret for trusted BFF / service callers ({@code X-Service-Key}). When blank,
   * service-key client identity is disabled and cookie flow is used.
   */
  private String apiKey = "";

  /** Tells whether service-to-service auth is on. */
  public boolean isEnabled() {
    return apiKey != null && !apiKey.isBlank();
  }
}
