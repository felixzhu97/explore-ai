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
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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
    private final SessionTitleGenerator sessionTitleGenerator;

    public SpringAiChatUseCase(
            ChatClient.Builder chatClientBuilder,
            ChatSessionRepository repository,
            RetryTemplate retryTemplate,
            ChatMemory chatMemory,
            SessionTitleGenerator sessionTitleGenerator) {
        this.chatClient = chatClientBuilder.build();
        this.repository = repository;
        this.retryTemplate = retryTemplate;
        this.chatMemory = chatMemory;
        this.sessionTitleGenerator = sessionTitleGenerator;
    }

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
            String response = chatClient.prompt()
                    .messages(messages.stream().map(this::toSpringMessage).toList())
                    .call()
                    .content();
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

    @Override
    public Flux<String> chatStreamWithSession(String sessionId, String userMessage) {
        ChatSession session = loadOrCreateSession(sessionId);
        boolean isFirstTurn = session.isEmpty();
        session.addUserMessage(userMessage);
        repository.save(session);

        ChatSessionId chatSessionId = session.getId();
        StringBuilder buffer = new StringBuilder();

        return chatStream(session.getMessages())
                .doOnNext(buffer::append)
                .doOnComplete(() -> persistAssistantReply(chatSessionId, buffer.toString(), isFirstTurn, userMessage))
                .doOnError(error -> log.error("Stream failed for session {}", sessionId, error));
    }

    private void persistAssistantReply(
            ChatSessionId sessionId,
            String assistantReply,
            boolean isFirstTurn,
            String userMessage) {
        if (assistantReply.isBlank()) {
            return;
        }
        repository.findById(sessionId).ifPresent(session -> {
            session.addAssistantMessage(assistantReply);
            repository.save(session);
            if (isFirstTurn && session.hasDefaultTitle()) {
                generateTitleAsync(sessionId, userMessage, assistantReply);
            }
        });
    }

    private void generateTitleAsync(ChatSessionId sessionId, String userMessage, String assistantReply) {
        Mono.fromCallable(() -> sessionTitleGenerator.generate(userMessage, assistantReply))
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        title -> repository.findById(sessionId).ifPresent(session -> {
                            if (session.hasDefaultTitle()) {
                                session.rename(title);
                                repository.save(session);
                                log.info("Renamed session {} to '{}'", sessionId, title);
                            }
                        }),
                        error -> log.warn("Async title generation failed for session {}", sessionId, error)
                );
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
        ChatSessionId id = ChatSessionId.of(sessionId);
        return repository.findById(id).orElseGet(() -> {
            ChatSession session = ChatSession.createWithId(id, ChatSession.DEFAULT_TITLE);
            repository.save(session);
            return session;
        });
    }

    private String exchangeMessages(ChatSession session, String userMessage) {
        session.addUserMessage(userMessage);
        repository.save(session);

        String aiResponse = chatWithHistory(session.getMessages());

        session.addAssistantMessage(aiResponse);
        repository.save(session);

        if (session.getUserMessageCount() == 1 && session.hasDefaultTitle()) {
            generateTitleAsync(session.getId(), userMessage, aiResponse);
        }

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

    public ChatClient getChatClient() {
        return chatClient;
    }

    public void clearConversationMemory(String conversationId) {
        chatMemory.clear(conversationId);
    }

    @Override
    public Flux<String> chatStream(List<ChatMessage> messages) {
        log.info("Streaming chat request with {} messages", messages.size());

        return chatClient.prompt()
                .messages(messages.stream().map(this::toSpringMessage).toList())
                .stream()
                .content();
    }

    private Message toSpringMessage(ChatMessage msg) {
        return msg.isFromUser()
                ? new UserMessage(msg.getText())
                : new AssistantMessage(msg.getText());
    }

    private String truncateForLog(String text) {
        if (text == null) {
            return "null";
        }
        if (text.length() <= 100) {
            return text;
        }
        return text.substring(0, 100) + "...";
    }
}
