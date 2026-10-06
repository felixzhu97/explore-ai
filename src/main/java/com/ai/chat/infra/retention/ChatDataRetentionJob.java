package com.ai.chat.infra.retention;

import com.ai.chat.domain.model.ChatSession;
import com.ai.chat.domain.repository.ChatSessionRepository;
import com.ai.chat.service.ChatSessionEraser;
import com.ai.common.infra.logging.LogSanitizer;
import com.ai.metrics.domain.repository.AiInvocationEventRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Purges inactive chat sessions and aged metrics events (GDPR storage limitation).
 *
 * @see <a href="https://gdpr.eu/article-5-how-to-process-personal-data/">GDPR Art.5</a>
 */
@Component
@EnableConfigurationProperties(DataRetentionProperties.class)
@RequiredArgsConstructor
public class ChatDataRetentionJob {

  private static final Logger log = LoggerFactory.getLogger(ChatDataRetentionJob.class);

  private final DataRetentionProperties properties;
  private final ChatSessionRepository sessionRepository;
  private final ChatSessionEraser sessionEraser;
  private final AiInvocationEventRepository invocationEventRepository;

  /** Deletes chat data older than the retention period. */
  @Scheduled(cron = "${app.data-retention.cron:0 0 3 * * *}")
  public void purgeExpiredData() {
    if (!properties.isEnabled()) {
      return;
    }
    Instant cutoff = Instant.now().minus(properties.getSessionMaxAge());
    List<ChatSession> expired = sessionRepository.findInactiveSince(cutoff);
    int metricsBySession = sessionEraser.eraseAll(expired);
    int metricsByAge = invocationEventRepository.deleteOlderThan(cutoff);
    log.info(
        "Retention purge cutoff={} sessions={} metricsBySession={} metricsByAge={}",
        cutoff,
        expired.size(),
        metricsBySession,
        metricsByAge);
    if (!expired.isEmpty()) {
      log.debug(
          "Purged sessionFp sample={}",
          LogSanitizer.fingerprint(expired.getFirst().getId().value()));
    }
  }
}
