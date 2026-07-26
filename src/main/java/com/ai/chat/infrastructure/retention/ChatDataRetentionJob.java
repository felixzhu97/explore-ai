package com.ai.chat.infrastructure.retention;

import com.ai.chat.domain.model.ChatSession;
import com.ai.chat.domain.repository.ChatSessionRepository;
import com.ai.chat.domain.repository.ChatWebSourcesRepository;
import com.ai.chat.domain.repository.ConversationMemoryRepository;
import com.ai.common.util.LogSanitizer;
import com.ai.metrics.domain.repository.AiInvocationEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Purges inactive chat sessions and aged metrics events (GDPR storage limitation).
 *
 * @see <a href="https://gdpr.eu/article-5-how-to-process-personal-data/">GDPR Art.5</a>
 */
@Component
@EnableConfigurationProperties(DataRetentionProperties.class)
public class ChatDataRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(ChatDataRetentionJob.class);

    private final DataRetentionProperties properties;
    private final ChatSessionRepository sessionRepository;
    private final ConversationMemoryRepository conversationMemoryRepository;
    private final ChatWebSourcesRepository chatWebSourcesRepository;
    private final AiInvocationEventRepository invocationEventRepository;

    public ChatDataRetentionJob(
            DataRetentionProperties properties,
            ChatSessionRepository sessionRepository,
            ConversationMemoryRepository conversationMemoryRepository,
            ChatWebSourcesRepository chatWebSourcesRepository,
            AiInvocationEventRepository invocationEventRepository) {
        this.properties = properties;
        this.sessionRepository = sessionRepository;
        this.conversationMemoryRepository = conversationMemoryRepository;
        this.chatWebSourcesRepository = chatWebSourcesRepository;
        this.invocationEventRepository = invocationEventRepository;
    }

    @Scheduled(cron = "${app.data-retention.cron:0 0 3 * * *}")
    public void purgeExpiredData() {
        if (!properties.isEnabled()) {
            return;
        }
        Instant cutoff = Instant.now().minus(properties.getSessionMaxAge());
        List<ChatSession> expired = sessionRepository.findInactiveSince(cutoff);
        List<String> sessionIds = expired.stream().map(session -> session.getId().value()).toList();

        for (ChatSession session : expired) {
            String sessionId = session.getId().value();
            conversationMemoryRepository.clear(sessionId);
            chatWebSourcesRepository.deleteByConversationId(sessionId);
            sessionRepository.delete(session.getId());
        }

        int metricsBySession = invocationEventRepository.deleteBySessionIds(sessionIds);
        int metricsByAge = invocationEventRepository.deleteOlderThan(cutoff);
        log.info(
                "Retention purge cutoff={} sessions={} metricsBySession={} metricsByAge={}",
                cutoff,
                expired.size(),
                metricsBySession,
                metricsByAge);
        if (!sessionIds.isEmpty()) {
            log.debug("Purged sessionFp sample={}", LogSanitizer.fingerprint(sessionIds.getFirst()));
        }
    }
}
