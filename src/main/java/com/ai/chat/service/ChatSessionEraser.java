package com.ai.chat.service;

import com.ai.account.domain.event.GuestLinkedToAccountEvent;
import com.ai.account.domain.event.OwnerErasureRequestedEvent;
import com.ai.chat.domain.event.ChatSessionsErasedEvent;
import com.ai.chat.domain.model.ChatSession;
import com.ai.chat.domain.repository.ChatSessionRepository;
import com.ai.chat.domain.repository.ChatWebSourcesRepository;
import com.ai.chat.domain.repository.ConversationMemoryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Erases chat sessions with everything tied to them: model memory and saved and pending web
 * sources. Data owned by other modules goes through {@link ChatSessionsErasedEvent}.
 */
@Service
@RequiredArgsConstructor
public class ChatSessionEraser {

  private final ChatSessionRepository sessionRepository;
  private final ConversationMemoryRepository conversationMemoryRepository;
  private final ChatWebSourcesRepository chatWebSourcesRepository;
  private final ApplicationEventPublisher eventPublisher;

  /**
   * Erases the sessions and returns how many were erased.
   *
   * @see <a href="https://gdpr.eu/article-17-right-to-be-forgotten/">GDPR Art.17</a>
   */
  public int eraseAll(List<ChatSession> sessions) {
    if (sessions.isEmpty()) {
      return 0;
    }
    List<String> sessionIds = sessions.stream().map(session -> session.getId().toString()).toList();
    // Session rows go last so a failed erase can be retried by finding the sessions again.
    eventPublisher.publishEvent(new ChatSessionsErasedEvent(sessionIds));
    for (ChatSession session : sessions) {
      String sessionId = session.getId().toString();
      conversationMemoryRepository.clearMessages(sessionId);
      chatWebSourcesRepository.deleteByConversationId(sessionId);
      CapturedWebSources.clearSources(sessionId);
      sessionRepository.deleteById(session.getId());
    }
    return sessions.size();
  }

  @EventListener
  @Transactional
  void handleOwnerErasureRequestedEvent(OwnerErasureRequestedEvent event) {
    eraseAll(sessionRepository.findAllByOwnerKeyOrderByLastActivityAtDesc(event.ownerKey()));
  }

  @EventListener
  @Transactional
  void handleGuestLinkedToAccountEvent(GuestLinkedToAccountEvent event) {
    sessionRepository.reassignOwnerKey(event.fromOwnerKey(), event.toOwnerKey());
  }
}
