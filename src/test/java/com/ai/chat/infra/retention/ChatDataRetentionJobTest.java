package com.ai.chat.infra.retention;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ai.chat.domain.model.ChatSession;
import com.ai.chat.domain.repository.ChatSessionRepository;
import com.ai.chat.domain.repository.ChatWebSourcesRepository;
import com.ai.chat.domain.repository.ConversationMemoryRepository;
import com.ai.chat.domain.vo.ChatSessionId;
import com.ai.metrics.domain.repository.AiInvocationEventRepository;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatDataRetentionJobTest {

  @Mock private ChatSessionRepository sessionRepository;

  @Mock private ConversationMemoryRepository conversationMemoryRepository;

  @Mock private ChatWebSourcesRepository chatWebSourcesRepository;

  @Mock private AiInvocationEventRepository invocationEventRepository;

  private DataRetentionProperties properties;
  private ChatDataRetentionJob job;

  @BeforeEach
  void setUp() {
    properties = new DataRetentionProperties();
    properties.setEnabled(true);
    properties.setSessionMaxAge(Duration.ofDays(90));
    job =
        new ChatDataRetentionJob(
            properties,
            sessionRepository,
            conversationMemoryRepository,
            chatWebSourcesRepository,
            invocationEventRepository);
  }

  @Test
  void shouldSkipPurgeWhenDisabled() {
    properties.setEnabled(false);

    job.purgeExpiredData();

    verify(sessionRepository, never()).findInactiveSince(any());
  }

  @Test
  void shouldPurgeInactiveSessionsAndMetricsWhenEnabled() {
    ChatSession expired =
        ChatSession.createWithId(
            ChatSessionId.of("33333333-3333-3333-3333-333333333333"), "Old", "c:client-a");
    when(sessionRepository.findInactiveSince(any())).thenReturn(List.of(expired));
    when(invocationEventRepository.deleteBySessionIds(anyCollection())).thenReturn(2);
    when(invocationEventRepository.deleteOlderThan(any())).thenReturn(1);

    job.purgeExpiredData();

    verify(conversationMemoryRepository).clear("33333333-3333-3333-3333-333333333333");
    verify(chatWebSourcesRepository).deleteByConversationId("33333333-3333-3333-3333-333333333333");
    verify(sessionRepository).delete(ChatSessionId.of("33333333-3333-3333-3333-333333333333"));
    verify(invocationEventRepository)
        .deleteBySessionIds(List.of("33333333-3333-3333-3333-333333333333"));
    verify(invocationEventRepository).deleteOlderThan(any());
  }
}
