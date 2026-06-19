package com.ai.domain.service;

import com.ai.domain.model.ChatMessage;
import com.ai.domain.model.ChatSession;
import com.ai.domain.model.ChatSessionNotFoundException;
import com.ai.domain.repository.ChatSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Domain service for AI chat operations using Spring AI ChatClient API.
 * Contains chat logic with retry and memory support.
 */
public class AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);

    private final ChatClient chatClient;
    private final ChatSessionRepository repository;
    private final RetryTemplate retryTemplate;
    private final ChatMemory chatMemory;

    public AiChatService(ChatClient.Builder chatClientBuilder, ChatSessionRepository repository,
                         RetryTemplate retryTemplate, ChatMemory chatMemory) {
        this.chatClient = chatClientBuilder.build();
        this.repository = repository;
        this.retryTemplate = retryTemplate;
        this.chatMemory = chatMemory;
    }

    /**
     * Sends a message to AI with retry support and returns the response.
     */
    public String chat(String userMessage) {
        log.info("Chat request with retry: {}", truncateForLog(userMessage));

        return retryTemplate.execute(context -> {
            if (context.getRetryCount() > 0) {
                log.info("Retry attempt {} for chat request", context.getRetryCount());
            }
            String response = chatClient.prompt()
                    .user(userMessage)
                    .call()
                    .content();
            log.info("Chat response: {}", truncateForLog(response));
            return response != null ? response : "";
        });
    }

    /**
     * Sends message history to AI with retry support.
     */
    public String chatWithHistory(List<ChatMessage> messages) {
        log.info("Chat request with {} messages", messages.size());

        return retryTemplate.execute(context -> {
            var promptBuilder = chatClient.prompt();

            for (ChatMessage msg : messages) {
                if (msg.isFromUser()) {
                    promptBuilder.user(msg.getText());
                } else {
                    promptBuilder.system(sp -> sp.text(msg.getText()));
                }
            }

            String response = promptBuilder.call().content();
            log.info("Chat response received: {} characters", response != null ? response.length() : 0);
            return response != null ? response : "";
        });
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
    public Optional<ChatSession> getSession(String sessionId) {
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

    /**
     * Get the underlying ChatClient for advanced operations like tool calling.
     */
    public ChatClient getChatClient() {
        return chatClient;
    }

    /**
     * Clear conversation memory for a specific conversation ID.
     */
    public void clearConversationMemory(String conversationId) {
        log.info("Clearing conversation memory for: {}", conversationId);
        chatMemory.clear(conversationId);
    }

    private String truncateForLog(String text) {
        if (text == null) return "null";
        if (text.length() <= 100) return text;
        return text.substring(0, 100) + "...";
    }
}
