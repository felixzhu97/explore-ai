package com.ai.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ai.account.domain.event.GuestLinkedToAccountEvent;
import com.ai.account.domain.event.OwnerErasureRequestedEvent;
import com.ai.chat.domain.event.ChatSessionsErasedEvent;
import com.ai.chat.domain.model.ChatSession;
import com.ai.chat.domain.model.ChatSessionId;
import com.ai.chat.domain.model.WebSource;
import com.ai.chat.domain.repository.ChatSessionRepository;
import com.ai.chat.domain.repository.ChatWebSourcesRepository;
import com.ai.chat.domain.repository.ConversationMemoryRepository;
import com.ai.common.domain.model.OwnerKey;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatSessionEraser")
class ChatSessionEraserTest {

  private static final String SESSION_ID = "55555555-5555-5555-5555-555555555555";
  private static final OwnerKey GUEST = OwnerKey.parseKey("c:client-a");
  private static final OwnerKey ACCOUNT = OwnerKey.parseKey("u:account-a");

  @Mock private ChatSessionRepository sessionRepository;
  @Mock private ConversationMemoryRepository conversationMemoryRepository;
  @Mock private ChatWebSourcesRepository chatWebSourcesRepository;
  @Mock private ApplicationEventPublisher eventPublisher;

  private ChatSessionEraser eraser;

  @BeforeEach
  void setUp() {
    eraser =
        new ChatSessionEraser(
            sessionRepository,
            conversationMemoryRepository,
            chatWebSourcesRepository,
            eventPublisher);
  }

  @Test
  @DisplayName("should erase memory, sources and pending sources with the session")
  void shouldEraseMemorySourcesAndPendingSourcesWithTheSession() {
    CapturedWebSources.saveSources(
        SESSION_ID, "query", List.of(new WebSource("Spring", "https://spring.io", "Docs")));

    int erased = eraser.eraseAll(List.of(session()));

    assertThat(erased).isEqualTo(1);
    assertThat(CapturedWebSources.getSources(SESSION_ID)).isNull();
    verify(eventPublisher).publishEvent(new ChatSessionsErasedEvent(List.of(SESSION_ID)));
    verify(conversationMemoryRepository).clearMessages(SESSION_ID);
    verify(chatWebSourcesRepository).deleteByConversationId(SESSION_ID);
    verify(sessionRepository).deleteById(ChatSessionId.parseId(SESSION_ID));
  }

  @Test
  @DisplayName("should delete the session row last when erasing")
  void shouldDeleteTheSessionRowLastWhenErasing() {
    eraser.eraseAll(List.of(session()));

    InOrder order =
        inOrder(
            eventPublisher,
            conversationMemoryRepository,
            chatWebSourcesRepository,
            sessionRepository);
    order.verify(eventPublisher).publishEvent(new ChatSessionsErasedEvent(List.of(SESSION_ID)));
    order.verify(conversationMemoryRepository).clearMessages(SESSION_ID);
    order.verify(chatWebSourcesRepository).deleteByConversationId(SESSION_ID);
    order.verify(sessionRepository).deleteById(ChatSessionId.parseId(SESSION_ID));
  }

  @Test
  @DisplayName("should touch nothing when there are no sessions to erase")
  void shouldTouchNothingWhenThereAreNoSessionsToErase() {
    assertThat(eraser.eraseAll(List.of())).isZero();
    verifyNoInteractions(
        sessionRepository, conversationMemoryRepository, chatWebSourcesRepository, eventPublisher);
  }

  @Test
  @DisplayName("should erase the owner's sessions when owner erasure is requested")
  void shouldEraseTheOwnersSessionsWhenOwnerErasureIsRequested() {
    when(sessionRepository.findAllByOwnerKeyOrderByLastActivityAtDesc(GUEST))
        .thenReturn(List.of(session()));

    eraser.handleOwnerErasureRequestedEvent(new OwnerErasureRequestedEvent(GUEST));

    verify(sessionRepository).deleteById(ChatSessionId.parseId(SESSION_ID));
  }

  @Test
  @DisplayName("should move the guest sessions to the account when a guest is linked")
  void shouldMoveTheGuestSessionsToTheAccountWhenAGuestIsLinked() {
    eraser.handleGuestLinkedToAccountEvent(new GuestLinkedToAccountEvent(GUEST, ACCOUNT));

    verify(sessionRepository).reassignOwnerKey(GUEST, ACCOUNT);
  }

  private static ChatSession session() {
    return ChatSession.restoreSession(
        ChatSessionId.parseId(SESSION_ID), "Old", Instant.now(), "c:client-a");
  }
}
