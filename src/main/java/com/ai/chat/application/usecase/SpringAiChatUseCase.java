package com.ai.chat.application.usecase;

import com.ai.chat.domain.exception.ChatSessionNotFoundException;
import com.ai.chat.domain.model.ChatMessage;
import com.ai.chat.domain.model.ChatSession;
import com.ai.chat.domain.repository.ChatSessionRepository;
import com.ai.chat.domain.repository.ChatWebSourcesRepository;
import com.ai.chat.domain.vo.ChatSessionId;
import com.ai.chat.domain.repository.ConversationMemoryRepository;
import com.ai.common.application.llm.ChatClientProvider;
import com.ai.common.application.llm.TextChatOptions;
import com.ai.common.domain.exception.AiServiceException;
import com.ai.common.infrastructure.llm.ToolEventChannel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SpringAiChatUseCase implements ChatUseCase {

    private static final Logger log = LoggerFactory.getLogger(SpringAiChatUseCase.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final ChatClientProvider chatClientProvider;
    private final ChatSessionRepository repository;
    private final RetryTemplate retryTemplate;
    private final ChatMemory chatMemory;
    private final ConversationMemoryRepository conversationMemoryRepository;
    private final SessionTitleGenerator sessionTitleGenerator;
    private final ChatWebSourcesRepository chatWebSourcesRepository;

    public SpringAiChatUseCase(
            ChatClientProvider chatClientProvider,
            ChatSessionRepository repository,
            RetryTemplate retryTemplate,
            ChatMemory chatMemory,
            ConversationMemoryRepository conversationMemoryRepository,
            SessionTitleGenerator sessionTitleGenerator,
            ChatWebSourcesRepository chatWebSourcesRepository) {
        this.chatClientProvider = chatClientProvider;
        this.repository = repository;
        this.retryTemplate = retryTemplate;
        this.chatMemory = chatMemory;
        this.conversationMemoryRepository = conversationMemoryRepository;
        this.sessionTitleGenerator = sessionTitleGenerator;
        this.chatWebSourcesRepository = chatWebSourcesRepository;
    }

    @Override
    public String chat(String userMessage) {
        return chat(userMessage, TextChatOptions.defaults());
    }

    @Override
    public String chat(String userMessage, TextChatOptions options) {
        log.info("Chat request with retry: {}", truncateForLog(userMessage));
        return retryTemplate.execute(context -> {
            ChatClient chatClient = chatClientProvider.createStateless(options);
            String response = chatClient.prompt()
                    .user(userMessage)
                    .call()
                    .content();
            if (response == null || response.isBlank()) {
                throw new AiServiceException("AI returned empty response");
            }
            return response;
        });
    }

    @Override
    public Flux<String> chatStreamWithSession(String sessionId, String userMessage) {
        return chatStreamWithSession(sessionId, userMessage, TextChatOptions.defaults());
    }

    @Override
    public Flux<String> chatStreamWithSession(String sessionId, String userMessage, TextChatOptions options) {
        return Flux.defer(() -> {
            ChatSession session = loadOrCreateSession(sessionId);
            boolean isFirstTurn = session.isEmpty();
            conversationMemoryRepository.seedIfEmpty(sessionId, session.getMessages());

            ToolEventChannel.setCurrentSessionId(sessionId);
            try {
                ChatClient chatClient = chatClientProvider.create(options, sessionId);
                return mergeToolEvents(
                        chatClient.prompt()
                                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, sessionId))
                                .user(userMessage)
                                .stream()
                                .content(),
                        sessionId,
                        options.toolsEnabled())
                        .doOnComplete(() -> Mono.fromRunnable(() ->
                                        afterSessionStream(session.getId(), sessionId, isFirstTurn, userMessage))
                                .subscribeOn(Schedulers.boundedElastic())
                                .subscribe())
                        .doOnError(error -> log.error("Stream failed for session {}", sessionId, error));
            } finally {
                ToolEventChannel.clearCurrentSessionId();
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private Flux<String> mergeToolEvents(Flux<String> content, String channelId, boolean toolsEnabled) {
        if (!toolsEnabled) {
            return content.map(this::messageEvent);
        }
        Sinks.Many<String> sink = ToolEventChannel.open(channelId);
        Flux<String> toolEvents = ToolEventChannel.asFlux(sink)
                .doOnNext(json -> captureSourcesEvent(channelId, json));
        Flux<String> textEvents = content.map(this::messageEvent)
                .doFinally(signal -> ToolEventChannel.close(channelId));
        return Flux.merge(toolEvents, textEvents);
    }

    private void captureSourcesEvent(String channelId, String json) {
        try {
            JsonNode root = JSON.readTree(json);
            if (root == null || !"sources".equals(root.path("type").asText())) {
                return;
            }
            CapturedWebSources.remember(
                    channelId,
                    root.path("query").asText(""),
                    CapturedWebSources.parseItems(root.get("items")));
        } catch (JsonProcessingException e) {
            log.debug("Skipping non-JSON tool event for sources capture");
        }
    }

    private String messageEvent(String token) {
        try {
            return JSON.writeValueAsString(Map.of("type", "message", "token", token == null ? "" : token));
        } catch (JsonProcessingException e) {
            return "{\"type\":\"message\",\"token\":\"\"}";
        }
    }

    private void afterSessionStream(ChatSessionId sessionId, String conversationId, boolean isFirstTurn, String userMessage) {
        repository.findById(sessionId).ifPresent(session -> {
            conversationMemoryRepository.syncToSession(conversationId, session);
            repository.save(session);
            persistCapturedSources(conversationId, session);
            if (isFirstTurn && session.hasDefaultTitle()) {
                String assistantReply = session.getLastAssistantMessage() != null
                        ? session.getLastAssistantMessage().getText()
                        : "";
                if (!assistantReply.isBlank()) {
                    generateTitleAsync(sessionId, userMessage, assistantReply);
                }
            }
        });
    }

    private void persistCapturedSources(String conversationId, ChatSession session) {
        CapturedWebSources.Capture capture = CapturedWebSources.take(conversationId);
        if (capture == null || capture.sources().isEmpty()) {
            return;
        }
        ChatMessage lastAssistant = session.getLastAssistantMessage();
        if (lastAssistant == null) {
            CapturedWebSources.clear(conversationId);
            return;
        }
        chatWebSourcesRepository.save(
                conversationId,
                lastAssistant.getText(),
                capture.query(),
                capture.sources());
    }

    @Override
    public String chatWithSession(String sessionId, String userMessage) {
        try {
            ChatSession session = loadOrCreateSession(sessionId);
            return exchangeMessages(session, sessionId, userMessage, TextChatOptions.defaults());
        } catch (ChatSessionNotFoundException e) {
            log.warn("Session not found, using default: {}", sessionId);
            return chatWithSession(userMessage);
        }
    }

    @Override
    public String chatWithSession(String userMessage) {
        ChatSession session = getOrCreateDefaultSession();
        return exchangeMessages(session, session.getId().value(), userMessage, TextChatOptions.defaults());
    }

    private String exchangeMessages(
            ChatSession session,
            String conversationId,
            String userMessage,
            TextChatOptions options) {
        conversationMemoryRepository.seedIfEmpty(conversationId, session.getMessages());
        boolean isFirstTurn = session.isEmpty();

        ChatClient chatClient = chatClientProvider.create(options, conversationId);
        String aiResponse = chatClient.prompt()
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                .user(userMessage)
                .call()
                .content();

        if (aiResponse == null || aiResponse.isBlank()) {
            throw new AiServiceException("AI returned empty response");
        }

        conversationMemoryRepository.syncToSession(conversationId, session);
        repository.save(session);

        if (isFirstTurn && session.hasDefaultTitle()) {
            generateTitleAsync(session.getId(), userMessage, aiResponse);
        }

        return aiResponse;
    }

    @Override
    public Flux<String> chatStream(List<ChatMessage> messages) {
        return chatStream(messages, TextChatOptions.defaults());
    }

    @Override
    public Flux<String> chatStream(List<ChatMessage> messages, TextChatOptions options) {
        String requestId = java.util.UUID.randomUUID().toString();
        ToolEventChannel.setCurrentSessionId(requestId);
        try {
            ChatClient chatClient = chatClientProvider.createStateless(options);
            return mergeToolEvents(
                    chatClient.prompt()
                            .messages(messages.stream().map(this::toSpringMessage).toList())
                            .stream()
                            .content(),
                    requestId,
                    options.toolsEnabled());
        } finally {
            ToolEventChannel.clearCurrentSessionId();
        }
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
        return sessions.getFirst();
    }

    private ChatSession loadOrCreateSession(String sessionId) {
        ChatSessionId id = ChatSessionId.of(sessionId);
        return repository.findById(id).orElseGet(() -> {
            ChatSession session = ChatSession.createWithId(id, ChatSession.DEFAULT_TITLE);
            repository.save(session);
            return session;
        });
    }

    @Override
    public ChatSession createSession(String title) {
        ChatSession session = ChatSession.create(title);
        repository.save(session);
        log.info("Created new session: {} with id: {}", title, session.getId());
        return session;
    }

    @Override
    public Optional<ChatSession> getSession(String sessionId) {
        return repository.findById(ChatSessionId.of(sessionId))
                .map(session -> {
                    conversationMemoryRepository.syncToSession(sessionId, session);
                    return session;
                });
    }

    @Override
    public List<ChatMessage> getSessionHistory(String sessionId) {
        ChatSession session = repository.findById(ChatSessionId.of(sessionId))
                .orElseThrow(() -> new ChatSessionNotFoundException(sessionId));
        conversationMemoryRepository.syncToSession(sessionId, session);
        return session.getMessages();
    }

    @Override
    public void deleteSession(String sessionId) {
        conversationMemoryRepository.clear(sessionId);
        chatWebSourcesRepository.deleteByConversationId(sessionId);
        CapturedWebSources.clear(sessionId);
        repository.delete(ChatSessionId.of(sessionId));
        log.info("Deleted session: {}", sessionId);
    }

    @Override
    public List<ChatSession> getAllSessions() {
        return repository.findAll().stream()
                .map(session -> {
                    conversationMemoryRepository.syncToSession(session.getId().value(), session);
                    return session;
                })
                .toList();
    }

    public void clearConversationMemory(String conversationId) {
        chatMemory.clear(conversationId);
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
