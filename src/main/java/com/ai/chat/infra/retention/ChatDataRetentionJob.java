package com.ai.chat.infra.retention;

import com.ai.chat.domain.model.ChatSession;
import com.ai.chat.domain.repository.ChatSessionRepository;
import com.ai.chat.service.ChatSessionEraser;
import com.ai.metrics.domain.repository.AiInvocationEventRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
    sessionEraser.eraseAll(expired);
    invocationEventRepository.deleteOlderThan(cutoff);
  }
}
