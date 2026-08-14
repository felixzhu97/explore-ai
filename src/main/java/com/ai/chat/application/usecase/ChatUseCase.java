package com.ai.chat.application.usecase;

import com.ai.chat.domain.model.ChatMessage;
import com.ai.chat.domain.model.ChatSession;
import com.ai.common.application.llm.TextChatOptions;
import java.util.List;
import java.util.Optional;
import reactor.core.publisher.Flux;

/** Documentation. */
public interface ChatUseCase {
  /** Documentation. */
  String chat(String userMessage);

  /** Documentation. */
  String chat(String userMessage, TextChatOptions options);

  /** Documentation. */
  Flux<String> chatStream(List<ChatMessage> messages);

  /** Documentation. */
  Flux<String> chatStream(List<ChatMessage> messages, TextChatOptions options);

  /** Documentation. */
  Flux<String> chatStreamWithSession(String sessionId, String userMessage, String clientId);

  /** Documentation. */
  Flux<String> chatStreamWithSession(
      String sessionId, String userMessage, TextChatOptions options, String clientId);

  /** Documentation. */
  String chatWithSession(String sessionId, String userMessage, String clientId);

  /** Documentation. */
  String chatWithSession(String userMessage, String clientId);

  /** Documentation. */
  ChatSession createSession(String title, String clientId);

  /** Documentation. */
  Optional<ChatSession> getSession(String sessionId, String clientId);

  /** Documentation. */
  List<ChatMessage> getSessionHistory(String sessionId, String clientId);

  /** Documentation. */
  void deleteSession(String sessionId, String clientId);

  /** Documentation. */
  void deleteAllSessionsForClient(String clientId);

  /** Documentation. */
  List<ChatSession> getSessionsForClient(String clientId);
}
