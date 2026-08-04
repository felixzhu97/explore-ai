package com.ai.chat.infrastructure;

import com.ai.chat.domain.ChatSession;
import com.ai.chat.domain.ChatSessionRepository;
import com.ai.chat.domain.ChatWebSourcesRepository;
import com.ai.chat.domain.ConversationMemoryRepository;
import com.ai.chat.domain.ChatSessionId;
import com.ai.metrics.domain.AiInvocationEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatDataRetentionJobTest {

    @Mock
    private ChatSessionRepository sessionRepository;

    @Mock
    private ConversationMemoryRepository conversationMemoryRepository;

    @Mock
    private ChatWebSourcesRepository chatWebSourcesRepository;

    @Mock
    private AiInvocationEventRepository invocationEventRepository;

    private DataRetentionProperties properties;
    private ChatDataRetentionJob job;

    @BeforeEach
    void setUp() {
        properties = new DataRetentionProperties();
        properties.setEnabled(true);
        properties.setSessionMaxAge(Duration.ofDays(90));
        job = new ChatDataRetentionJob(
                properties,
                sessionRepository,
                conversationMemoryRepository,
                chatWebSourcesRepository,
                invocationEventRepository);
    }

    @Test
    void should_skipPurge_whenDisabled() {
        properties.setEnabled(false);

        job.purgeExpiredData();

        verify(sessionRepository, never()).findInactiveSince(any());
    }

    @Test
    void should_purgeInactiveSessionsAndMetrics_whenEnabled() {
        ChatSession expired = ChatSession.createWithId(ChatSessionId.of("old-session"), "Old", "client-a");
        when(sessionRepository.findInactiveSince(any())).thenReturn(List.of(expired));
        when(invocationEventRepository.deleteBySessionIds(anyCollection())).thenReturn(2);
        when(invocationEventRepository.deleteOlderThan(any())).thenReturn(1);

        job.purgeExpiredData();

        verify(conversationMemoryRepository).clear("old-session");
        verify(chatWebSourcesRepository).deleteByConversationId("old-session");
        verify(sessionRepository).delete(ChatSessionId.of("old-session"));
        verify(invocationEventRepository).deleteBySessionIds(List.of("old-session"));
        verify(invocationEventRepository).deleteOlderThan(any());
    }
}
