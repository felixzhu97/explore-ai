package com.ai.chat.application.usecase;

import com.ai.chat.domain.model.ChatMessage;
import com.ai.chat.domain.model.ChatSession;
import com.ai.common.application.llm.TextChatOptions;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;

public interface ChatUseCase {
    String chat(String userMessage);

    String chat(String userMessage, TextChatOptions options);

    Flux<String> chatStream(List<ChatMessage> messages);

    Flux<String> chatStream(List<ChatMessage> messages, TextChatOptions options);

    Flux<String> chatStreamWithSession(String sessionId, String userMessage, String clientId);

    Flux<String> chatStreamWithSession(
            String sessionId, String userMessage, TextChatOptions options, String clientId);

    String chatWithSession(String sessionId, String userMessage, String clientId);

    String chatWithSession(String userMessage, String clientId);

    ChatSession createSession(String title, String clientId);

    Optional<ChatSession> getSession(String sessionId, String clientId);

    List<ChatMessage> getSessionHistory(String sessionId, String clientId);

    void deleteSession(String sessionId, String clientId);

    List<ChatSession> getSessionsForClient(String clientId);
}
