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

    Flux<String> chatStreamWithSession(String sessionId, String userMessage);

    Flux<String> chatStreamWithSession(String sessionId, String userMessage, TextChatOptions options);

    String chatWithSession(String sessionId, String userMessage);

    String chatWithSession(String userMessage);

    ChatSession createSession(String title);

    Optional<ChatSession> getSession(String sessionId);

    List<ChatMessage> getSessionHistory(String sessionId);

    void deleteSession(String sessionId);

    List<ChatSession> getAllSessions();
}
