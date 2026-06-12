package com.ai.application.service;

import com.ai.application.port.AiChatPort;
import com.ai.application.port.ChatSessionRepositoryPort;
import com.ai.application.usecase.SendChatMessageUseCase;
import com.ai.domain.model.ChatMessage;
import com.ai.domain.model.ChatSession;
import com.ai.domain.model.ChatSessionNotFoundException;
import com.ai.domain.vo.ChatSessionId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Chat application service - orchestrates use cases and manages sessions.
 */
public class ChatApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ChatApplicationService.class);

    private final ChatSessionRepositoryPort repositoryPort;
    private final AiChatPort aiChatPort;
    private final SendChatMessageUseCase sendChatMessageUseCase;

    public ChatApplicationService(
            ChatSessionRepositoryPort repositoryPort,
            AiChatPort aiChatPort,
            SendChatMessageUseCase sendChatMessageUseCase) {
        this.repositoryPort = repositoryPort;
        this.aiChatPort = aiChatPort;
        this.sendChatMessageUseCase = sendChatMessageUseCase;
    }

    /**
     * Processes a user message and returns AI response.
     */
    public String processChatMessage(String sessionId, String userMessage) {
        log.info("Processing chat message for session: {}", sessionId);

        try {
            ChatMessage response = sendChatMessageUseCase.execute(sessionId, userMessage);
            return response.getText();
        } catch (ChatSessionNotFoundException e) {
            log.warn("Session not found, creating new session: {}", sessionId);
            ChatMessage response = sendChatMessageUseCase.executeInDefaultSession(userMessage);
            return response.getText();
        }
    }

    /**
     * Processes a user message using a default session.
     */
    public String processChatMessage(String userMessage) {
        log.info("Processing chat message in default session");
        ChatMessage response = sendChatMessageUseCase.executeInDefaultSession(userMessage);
        return response.getText();
    }

    /**
     * Creates a new session.
     */
    public ChatSession createSession(String title) {
        ChatSession session = ChatSession.create(title);
        repositoryPort.save(session);
        log.info("Created new session: {} with id: {}", title, session.getId());
        return session;
    }

    /**
     * Creates a new session with default title.
     */
    public ChatSession createSession() {
        return createSession("New Chat");
    }

    /**
     * Retrieves a session by ID.
     */
    public Optional<ChatSession> getSession(String sessionId) {
        return repositoryPort.findById(ChatSessionId.of(sessionId));
    }

    /**
     * Retrieves message history for a session.
     */
    public List<ChatMessage> getSessionHistory(String sessionId) {
        return repositoryPort.findById(ChatSessionId.of(sessionId))
            .map(ChatSession::getMessages)
            .orElseThrow(() -> new ChatSessionNotFoundException(sessionId));
    }

    /**
     * Retrieves the most recent messages.
     */
    public List<ChatMessage> getRecentMessages(String sessionId, int count) {
        return repositoryPort.findById(ChatSessionId.of(sessionId))
            .map(session -> session.getRecentMessages(count))
            .orElseThrow(() -> new ChatSessionNotFoundException(sessionId));
    }

    /**
     * Deletes a session.
     */
    public void deleteSession(String sessionId) {
        repositoryPort.delete(ChatSessionId.of(sessionId));
        log.info("Deleted session: {}", sessionId);
    }

    /**
     * Retrieves all sessions.
     */
    public List<ChatSession> getAllSessions() {
        return repositoryPort.findAll();
    }
}
