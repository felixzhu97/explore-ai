package com.ai.chat.service;

import com.ai.chat.domain.model.ChatSession;
import com.ai.chat.domain.repository.ChatSessionRepository;
import com.ai.chat.domain.repository.ChatWebSourcesRepository;
import com.ai.chat.domain.repository.ConversationMemoryRepository;
import com.ai.metrics.domain.repository.AiInvocationEventRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Erases chat sessions with everything tied to them: model memory, saved and pending web sources,
 * and the metrics events recorded for them.
 */
@Service
@RequiredArgsConstructor
public class ChatSessionEraser {

  private final ChatSessionRepository sessionRepository;
  private final ConversationMemoryRepository conversationMemoryRepository;
  private final ChatWebSourcesRepository chatWebSourcesRepository;
  private final AiInvocationEventRepository invocationEventRepository;

  /**
   * Erases the sessions and returns how many metrics events went with them.
   *
   * @see <a href="https://gdpr.eu/article-17-right-to-be-forgotten/">GDPR Art.17</a>
   */
  public int eraseAll(List<ChatSession> sessions) {
    if (sessions.isEmpty()) {
      return 0;
    }
    List<String> sessionIds = sessions.stream().map(session -> session.getId().toString()).toList();
    // Session rows go last so a failed erase can be retried by finding the sessions again.
    int metricsDeleted = invocationEventRepository.deleteBySessionIds(sessionIds);
    for (ChatSession session : sessions) {
      String sessionId = session.getId().toString();
      conversationMemoryRepository.clearMessages(sessionId);
      chatWebSourcesRepository.deleteByConversationId(sessionId);
      CapturedWebSources.clearSources(sessionId);
      sessionRepository.deleteById(session.getId());
    }
    return metricsDeleted;
  }
}
