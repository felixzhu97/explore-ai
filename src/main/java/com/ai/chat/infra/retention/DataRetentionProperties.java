package com.ai.chat.infra.retention;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Documentation. */
@ConfigurationProperties(prefix = "app.data-retention")
public class DataRetentionProperties {

  private boolean enabled = true;
  private Duration sessionMaxAge = Duration.ofDays(90);
  private String cron = "0 0 3 * * *";

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public Duration getSessionMaxAge() {
    return sessionMaxAge;
  }

  public void setSessionMaxAge(Duration sessionMaxAge) {
    this.sessionMaxAge = sessionMaxAge;
  }

  public String getCron() {
    return cron;
  }

  public void setCron(String cron) {
    this.cron = cron;
  }
}
