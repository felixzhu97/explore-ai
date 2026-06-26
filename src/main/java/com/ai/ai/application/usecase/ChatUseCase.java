package com.ai.ai.application.usecase;

import com.ai.ai.domain.model.ChatMessage;
import com.ai.ai.domain.model.ChatSession;
import java.util.List;
import java.util.Optional;

public interface ChatUseCase {
    String chat(String userMessage);
    String chatWithSession(String sessionId, String userMessage);
    String chatWithSession(String userMessage);
    ChatSession createSession(String title);
    Optional<ChatSession> getSession(String sessionId);
    List<ChatMessage> getSessionHistory(String sessionId);
    void deleteSession(String sessionId);
    List<ChatSession> getAllSessions();
}
