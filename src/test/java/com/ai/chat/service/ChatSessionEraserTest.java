package com.ai.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ai.chat.domain.model.ChatSession;
import com.ai.chat.domain.model.ChatSessionId;
import com.ai.chat.domain.model.WebSource;
import com.ai.chat.domain.repository.ChatSessionRepository;
import com.ai.chat.domain.repository.ChatWebSourcesRepository;
import com.ai.chat.domain.repository.ConversationMemoryRepository;
import com.ai.metrics.domain.repository.AiInvocationEventRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatSessionEraser")
class ChatSessionEraserTest {

  private static final String SESSION_ID = "55555555-5555-5555-5555-555555555555";

  @Mock private ChatSessionRepository sessionRepository;
  @Mock private ConversationMemoryRepository conversationMemoryRepository;
  @Mock private ChatWebSourcesRepository chatWebSourcesRepository;
  @Mock private AiInvocationEventRepository invocationEventRepository;

  private ChatSessionEraser eraser;

  @BeforeEach
  void setUp() {
    eraser =
        new ChatSessionEraser(
            sessionRepository,
            conversationMemoryRepository,
            chatWebSourcesRepository,
            invocationEventRepository);
  }

  @Test
  @DisplayName("should erase memory, sources, pending sources and metrics with the session")
  void shouldEraseMemorySourcesPendingSourcesAndMetricsWithTheSession() {
    ChatSession session =
        ChatSession.restoreSession(
            ChatSessionId.parseId(SESSION_ID), "Old", Instant.now(), "c:client-a");
    CapturedWebSources.saveSources(
        SESSION_ID, "query", List.of(new WebSource("Spring", "https://spring.io", "Docs")));
    when(invocationEventRepository.deleteBySessionIds(List.of(SESSION_ID))).thenReturn(3);

    int metricsDeleted = eraser.eraseAll(List.of(session));

    assertThat(metricsDeleted).isEqualTo(3);
    assertThat(CapturedWebSources.getSources(SESSION_ID)).isNull();
    verify(conversationMemoryRepository).clearMessages(SESSION_ID);
    verify(chatWebSourcesRepository).deleteByConversationId(SESSION_ID);
    verify(sessionRepository).deleteById(ChatSessionId.parseId(SESSION_ID));
  }

  @Test
  @DisplayName("should delete the session row last when erasing")
  void shouldDeleteTheSessionRowLastWhenErasing() {
    ChatSession session =
        ChatSession.restoreSession(
            ChatSessionId.parseId(SESSION_ID), "Old", Instant.now(), "c:client-a");

    eraser.eraseAll(List.of(session));

    InOrder order =
        inOrder(
            invocationEventRepository,
            conversationMemoryRepository,
            chatWebSourcesRepository,
            sessionRepository);
    order.verify(invocationEventRepository).deleteBySessionIds(List.of(SESSION_ID));
    order.verify(conversationMemoryRepository).clearMessages(SESSION_ID);
    order.verify(chatWebSourcesRepository).deleteByConversationId(SESSION_ID);
    order.verify(sessionRepository).deleteById(ChatSessionId.parseId(SESSION_ID));
  }

  @Test
  @DisplayName("should touch nothing when there are no sessions to erase")
  void shouldTouchNothingWhenThereAreNoSessionsToErase() {
    assertThat(eraser.eraseAll(List.of())).isZero();
    verifyNoInteractions(
        sessionRepository,
        conversationMemoryRepository,
        chatWebSourcesRepository,
        invocationEventRepository);
  }
}
