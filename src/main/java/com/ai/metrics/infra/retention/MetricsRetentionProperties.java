package com.ai.metrics.infra.retention;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Retention for AI invocation events; shares the chat data retention settings. */
@ConfigurationProperties(prefix = "app.data-retention")
@Getter
@Setter
public class MetricsRetentionProperties {
  private boolean enabled = true;
  private Duration sessionMaxAge = Duration.ofDays(90);
}
