package com.ai.metrics.infra.retention;

import com.ai.metrics.domain.repository.AiInvocationEventRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Purges aged AI invocation events (GDPR storage limitation).
 *
 * @see <a href="https://gdpr.eu/article-5-how-to-process-personal-data/">GDPR Art.5</a>
 */
@Component
@EnableConfigurationProperties(MetricsRetentionProperties.class)
@RequiredArgsConstructor
public class MetricsRetentionJob {

  private final MetricsRetentionProperties properties;
  private final AiInvocationEventRepository invocationEventRepository;

  /** Deletes invocation events older than the retention period. */
  @Scheduled(cron = "${app.data-retention.cron:0 0 3 * * *}")
  public void purgeExpiredEvents() {
    if (!properties.isEnabled()) {
      return;
    }
    invocationEventRepository.deleteOlderThan(Instant.now().minus(properties.getSessionMaxAge()));
  }
}
