package com.ai.chat.application.usecase;

import com.ai.chat.domain.model.ChatMessage;
import com.ai.chat.domain.model.ChatSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Facade for chat and session management operations.
 */
@Service
public class ChatFacade {

    private static final Logger log = LoggerFactory.getLogger(ChatFacade.class);

    private final SpringAiChatUseCase chatUseCase;

    public ChatFacade(SpringAiChatUseCase chatUseCase) {
        this.chatUseCase = chatUseCase;
    }

    /**
     * Simple chat without session context.
     */
    public String chat(String message) {
        log.info("ChatFacade.chat: {}", truncate(message));
        return chatUseCase.chat(message);
    }

    /**
     * Chat with session context.
     */
    public String chatWithSession(String sessionId, String message) {
        log.info("ChatFacade.chatWithSession: session={}, message={}", sessionId, truncate(message));
        return chatUseCase.chatWithSession(sessionId, message);
    }

    /**
     * Chat with default session.
     */
    public String chatWithSession(String message) {
        log.info("ChatFacade.chatWithSession (default): {}", truncate(message));
        return chatUseCase.chatWithSession(message);
    }

    /**
     * Create a new chat session.
     */
    public ChatSession createSession(String title) {
        log.info("ChatFacade.createSession: {}", title);
        return chatUseCase.createSession(title);
    }

    /**
     * Get session by ID.
     */
    public Optional<ChatSession> getSession(String sessionId) {
        log.info("ChatFacade.getSession: {}", sessionId);
        return chatUseCase.getSession(sessionId);
    }

    /**
     * Get session message history.
     */
    public List<ChatMessage> getSessionHistory(String sessionId) {
        log.info("ChatFacade.getSessionHistory: {}", sessionId);
        return chatUseCase.getSessionHistory(sessionId);
    }

    /**
     * Delete a session.
     */
    public void deleteSession(String sessionId) {
        log.info("ChatFacade.deleteSession: {}", sessionId);
        chatUseCase.deleteSession(sessionId);
    }

    /**
     * Get all sessions.
     */
    public List<ChatSession> getAllSessions() {
        log.info("ChatFacade.getAllSessions");
        return chatUseCase.getAllSessions();
    }

    private String truncate(String text) {
        if (text == null) return "null";
        if (text.length() <= 50) return text;
        return text.substring(0, 50) + "...";
    }
}
