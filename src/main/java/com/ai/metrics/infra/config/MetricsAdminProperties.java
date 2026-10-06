package com.ai.metrics.infra.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration properties under {@code app.metrics} holding the optional admin API key. */
@ConfigurationProperties(prefix = "app.metrics")
@Getter
@Setter
public class MetricsAdminProperties {

  /**
   * When non-blank, {@code /api/metrics/**} requires matching {@code X-Admin-Key}. Leave empty in
   * local/dev so the Metrics UI works without a secret.
   */
  private String adminApiKey = "";

  /** Tells whether the admin API key is set. */
  public boolean isAuthEnabled() {
    return adminApiKey != null && !adminApiKey.isBlank();
  }
}
