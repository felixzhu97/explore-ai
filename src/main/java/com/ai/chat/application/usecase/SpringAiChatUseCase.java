package com.ai.chat.application.usecase;

import com.ai.chat.domain.model.ChatMessage;
import com.ai.chat.domain.model.ChatSession;
import com.ai.chat.domain.exception.ChatSessionNotFoundException;
import com.ai.chat.domain.repository.ChatSessionRepository;
import com.ai.chat.domain.vo.ChatSessionId;
import com.ai.common.domain.exception.AiServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Spring AI implementation of ChatUseCase using ChatClient API.
 */
@Service
public class SpringAiChatUseCase implements ChatUseCase {

    private static final Logger log = LoggerFactory.getLogger(SpringAiChatUseCase.class);

    private final ChatClient chatClient;
    private final ChatSessionRepository repository;
    private final RetryTemplate retryTemplate;
    private final ChatMemory chatMemory;

    public SpringAiChatUseCase(ChatClient.Builder chatClientBuilder, ChatSessionRepository repository,
                         RetryTemplate retryTemplate, ChatMemory chatMemory) {
        this.chatClient = chatClientBuilder.build();
        this.repository = repository;
        this.retryTemplate = retryTemplate;
        this.chatMemory = chatMemory;
    }

    /**
     * Sends a message to AI with retry support and returns the response.
     */
    @Override
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
            if (response == null || response.isBlank()) {
                throw new AiServiceException("AI returned empty response");
            }
            return response;
        });
    }

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

            var response = promptBuilder.call().content();
            if (response == null || response.isBlank()) {
                throw new AiServiceException("AI returned empty response");
            }
            return response;
        });
    }

    @Override
    public String chatWithSession(String sessionId, String userMessage) {
        try {
            ChatSession session = loadOrCreateSession(sessionId);
            return exchangeMessages(session, userMessage);
        } catch (ChatSessionNotFoundException e) {
            log.warn("Session not found, using default: {}", sessionId);
            return chatWithSession(userMessage);
        }
    }

    @Override
    public String chatWithSession(String userMessage) {
        ChatSession session = getOrCreateDefaultSession();
        return exchangeMessages(session, userMessage);
    }

    private ChatSession getOrCreateDefaultSession() {
        List<ChatSession> sessions = repository.findAll();
        if (sessions.isEmpty()) {
            ChatSession newSession = ChatSession.create("Default Chat");
            repository.save(newSession);
            return newSession;
        }
        return sessions.get(0);
    }

    private ChatSession loadOrCreateSession(String sessionId) {
        return repository.findById(ChatSessionId.of(sessionId))
                .orElseThrow(() -> new ChatSessionNotFoundException(sessionId));
    }

    private String exchangeMessages(ChatSession session, String userMessage) {
        session.addUserMessage(userMessage);
        repository.save(session);

        String aiResponse = chat(userMessage);

        session.addAssistantMessage(aiResponse);
        repository.save(session);

        return aiResponse;
    }

    @Override
    public ChatSession createSession(String title) {
        ChatSession session = ChatSession.create(title);
        repository.save(session);
        log.info("Created new session: {} with id: {}", title, session.getId());
        return session;
    }

    public ChatSession createSession() {
        return createSession("New Chat");
    }

    @Override
    public Optional<ChatSession> getSession(String sessionId) {
        return repository.findById(ChatSessionId.of(sessionId));
    }

    @Override
    public List<ChatMessage> getSessionHistory(String sessionId) {
        return repository.findById(ChatSessionId.of(sessionId))
                .map(ChatSession::getMessages)
                .orElseThrow(() -> new ChatSessionNotFoundException(sessionId));
    }

    public List<ChatMessage> getRecentMessages(String sessionId, int count) {
        return repository.findById(ChatSessionId.of(sessionId))
                .map(session -> session.getRecentMessages(count))
                .orElseThrow(() -> new ChatSessionNotFoundException(sessionId));
    }

    @Override
    public void deleteSession(String sessionId) {
        repository.delete(ChatSessionId.of(sessionId));
        log.info("Deleted session: {}", sessionId);
    }

    @Override
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
        chatMemory.clear(conversationId);
    }

    private String truncateForLog(String text) {
        if (text == null) return "null";
        if (text.length() <= 100) return text;
        return text.substring(0, 100) + "...";
    }
}
