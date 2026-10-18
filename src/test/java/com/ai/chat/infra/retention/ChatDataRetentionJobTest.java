package com.ai.chat.infra.retention;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ai.chat.domain.event.ChatSessionsErasedEvent;
import com.ai.chat.domain.model.ChatSession;
import com.ai.chat.domain.model.ChatSessionId;
import com.ai.chat.domain.repository.ChatSessionRepository;
import com.ai.chat.domain.repository.ChatWebSourcesRepository;
import com.ai.chat.domain.repository.ConversationMemoryRepository;
import com.ai.chat.service.ChatSessionEraser;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatDataRetentionJob")
class ChatDataRetentionJobTest {

  @Mock private ChatSessionRepository sessionRepository;

  @Mock private ConversationMemoryRepository conversationMemoryRepository;

  @Mock private ChatWebSourcesRepository chatWebSourcesRepository;

  @Mock private ApplicationEventPublisher eventPublisher;

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
            new ChatSessionEraser(
                sessionRepository,
                conversationMemoryRepository,
                chatWebSourcesRepository,
                eventPublisher));
  }

  @Test
  void shouldSkipPurgeWhenDisabled() {
    properties.setEnabled(false);

    job.purgeExpiredData();

    verify(sessionRepository, never()).findAllByLastActivityAtBeforeOrderByLastActivityAtAsc(any());
  }

  @Test
  void shouldPurgeInactiveSessionsWhenEnabled() {
    ChatSession expired =
        ChatSession.restoreSession(
            ChatSessionId.parseId("33333333-3333-3333-3333-333333333333"),
            "Old",
            java.time.Instant.now(),
            "c:client-a");
    when(sessionRepository.findAllByLastActivityAtBeforeOrderByLastActivityAtAsc(any()))
        .thenReturn(List.of(expired));

    job.purgeExpiredData();

    verify(conversationMemoryRepository).clearMessages("33333333-3333-3333-3333-333333333333");
    verify(chatWebSourcesRepository).deleteByConversationId("33333333-3333-3333-3333-333333333333");
    verify(sessionRepository)
        .deleteById(ChatSessionId.parseId("33333333-3333-3333-3333-333333333333"));
    verify(eventPublisher)
        .publishEvent(new ChatSessionsErasedEvent(List.of("33333333-3333-3333-3333-333333333333")));
  }
}
