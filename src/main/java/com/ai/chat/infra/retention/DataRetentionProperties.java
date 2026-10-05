package com.ai.chat.infra.retention;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Settings under {@code app.data-retention}: purge toggle, max session age, and cron. */
@ConfigurationProperties(prefix = "app.data-retention")
@Getter
@Setter
public class DataRetentionProperties {

  private boolean enabled = true;
  private Duration sessionMaxAge = Duration.ofDays(90);
  private String cron = "0 0 3 * * *";
}
