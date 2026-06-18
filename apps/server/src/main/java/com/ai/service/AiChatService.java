package com.ai.service;

import com.ai.domain.model.ChatMessage;
import com.ai.domain.model.ChatSession;
import com.ai.domain.model.ChatSessionNotFoundException;
import com.ai.domain.repository.ChatSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Simplified AI Chat Service.
 * Single service containing business logic for AI chat interactions.
 */
@Service
public class AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);

    private final ChatModel chatModel;
    private final ChatSessionRepository repository;

    public AiChatService(
            @Qualifier("openAiChatModel") ChatModel chatModel,
            ChatSessionRepository repository) {
        this.chatModel = chatModel;
        this.repository = repository;
    }

    /**
     * Sends a message to AI and returns the response.
     */
    public String chat(String userMessage) {
        log.info("Simple chat request: {}", truncateForLog(userMessage));
        UserMessage userMsg = new UserMessage(userMessage);
        Prompt prompt = new Prompt(userMsg);

        try {
            ChatResponse response = chatModel.call(prompt);
            String text = extractText(response);
            log.info("Chat response: {}", truncateForLog(text));
            return text;
        } catch (Exception e) {
            log.error("Chat error", e);
            throw new RuntimeException("AI service error: " + e.getMessage(), e);
        }
    }

    /**
     * Sends message history to AI and returns the response.
     */
    public String chatWithHistory(List<ChatMessage> messages) {
        log.info("Chat request with {} messages", messages.size());

        List<org.springframework.ai.chat.messages.Message> springMessages = new ArrayList<>();
        for (ChatMessage msg : messages) {
            if (msg.isFromUser()) {
                springMessages.add(new UserMessage(msg.getText()));
            } else {
                springMessages.add(new AssistantMessage(msg.getText()));
            }
        }

        Prompt prompt = new Prompt(springMessages);

        try {
            ChatResponse response = chatModel.call(prompt);
            String text = extractText(response);
            log.info("Chat response received: {} characters", text.length());
            return text;
        } catch (Exception e) {
            log.error("Chat error", e);
            throw new RuntimeException("AI service error: " + e.getMessage(), e);
        }
    }

    /**
     * Processes a chat message in a session.
     */
    public String processChatMessage(String sessionId, String userMessage) {
        log.info("Processing chat message for session: {}", sessionId);

        try {
            ChatSession session = repository.findById(
                    com.ai.domain.vo.ChatSessionId.of(sessionId))
                .orElseThrow(() -> new ChatSessionNotFoundException(sessionId));

            session.addUserMessage(userMessage);
            repository.save(session);

            String aiResponse = chat(userMessage);

            session.addAssistantMessage(aiResponse);
            repository.save(session);

            return aiResponse;
        } catch (ChatSessionNotFoundException e) {
            log.warn("Session not found, using default: {}", sessionId);
            return processChatMessage(userMessage);
        }
    }

    /**
     * Processes a chat message in the default session.
     */
    public String processChatMessage(String userMessage) {
        log.info("Processing chat message in default session");
        
        ChatSession session = repository.getOrCreateDefaultSession();
        session.addUserMessage(userMessage);

        String aiResponse = chat(userMessage);

        session.addAssistantMessage(aiResponse);
        repository.save(session);

        return aiResponse;
    }

    /**
     * Creates a new session.
     */
    public ChatSession createSession(String title) {
        ChatSession session = ChatSession.create(title);
        repository.save(session);
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
    public java.util.Optional<ChatSession> getSession(String sessionId) {
        return repository.findById(com.ai.domain.vo.ChatSessionId.of(sessionId));
    }

    /**
     * Retrieves message history for a session.
     */
    public List<ChatMessage> getSessionHistory(String sessionId) {
        return repository.findById(com.ai.domain.vo.ChatSessionId.of(sessionId))
            .map(ChatSession::getMessages)
            .orElseThrow(() -> new ChatSessionNotFoundException(sessionId));
    }

    /**
     * Retrieves the most recent messages.
     */
    public List<ChatMessage> getRecentMessages(String sessionId, int count) {
        return repository.findById(com.ai.domain.vo.ChatSessionId.of(sessionId))
            .map(session -> session.getRecentMessages(count))
            .orElseThrow(() -> new ChatSessionNotFoundException(sessionId));
    }

    /**
     * Deletes a session.
     */
    public void deleteSession(String sessionId) {
        repository.delete(com.ai.domain.vo.ChatSessionId.of(sessionId));
        log.info("Deleted session: {}", sessionId);
    }

    /**
     * Retrieves all sessions.
     */
    public List<ChatSession> getAllSessions() {
        return repository.findAll();
    }

    private String extractText(ChatResponse response) {
        String text = response.getResult().getOutput().getText();
        return text != null ? text : "";
    }

    private String truncateForLog(String text) {
        if (text == null) return "null";
        if (text.length() <= 100) return text;
        return text.substring(0, 100) + "...";
    }
}
