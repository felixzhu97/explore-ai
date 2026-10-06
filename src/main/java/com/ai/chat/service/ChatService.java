package com.ai.chat.service;

import com.ai.chat.domain.exception.ChatSessionNotFoundException;
import com.ai.chat.domain.model.ChatMessage;
import com.ai.chat.domain.model.ChatSession;
import com.ai.chat.domain.repository.ChatSessionRepository;
import com.ai.chat.domain.repository.ChatWebSourcesRepository;
import com.ai.chat.domain.repository.ConversationMemoryRepository;
import com.ai.chat.domain.vo.ChatSessionId;
import com.ai.common.domain.exception.AiServiceException;
import com.ai.common.infra.llm.ToolCallMarkupFilter;
import com.ai.common.infra.llm.ToolEventChannel;
import com.ai.common.infra.logging.LogSanitizer;
import com.ai.common.infra.prompt.PromptTemplates;
import com.ai.common.service.llm.ChatClientProvider;
import com.ai.common.service.llm.StreamTokenEvent;
import com.ai.common.service.llm.TextChatOptions;
import com.ai.metrics.domain.vo.AiDomain;
import com.ai.metrics.service.AiInvocationRecorder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

/** Spring AI chat use case with session memory, tool calls, web sources, and usage metrics. */
@Service
@RequiredArgsConstructor
public class ChatService {

  private static final Logger log = LoggerFactory.getLogger(ChatService.class);
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final String REPAIR_USER_PROMPT =
      """
            Produce the final answer now using the web search results provided in this prompt.
            Reply in the user's language. If a chart was requested, emit the a2ui fence with
            chartData from those results.
            Do not call tools. Do not emit DSML or tool-call markup.
            Do not say search results are missing.
            """;

  private final ChatClientProvider chatClientProvider;
  private final ChatSessionRepository repository;
  private final RetryTemplate retryTemplate;
  private final ChatMemory chatMemory;
  private final ConversationMemoryRepository conversationMemoryRepository;
  private final SessionTitleGenerator sessionTitleGenerator;
  private final ChatWebSourcesRepository chatWebSourcesRepository;
  private final PromptTemplates promptTemplates;
  private final AiInvocationRecorder invocationRecorder;
  private final ChatSessionEraser sessionEraser;

  /** Returns the client's sessions, most recently active first. */
  public List<ChatSession> listSessions(String ownerKey) {
    return repository.findByOwnerKey(ownerKey).stream().map(this::withStoredMessages).toList();
  }

  /**
   * Returns the client's session messages in chronological order with their web sources. The owner
   * is checked before any sources are read.
   */
  public SessionHistory findSessionHistoryWithSources(String sessionId, String ownerKey) {
    ChatSession session =
        repository
            .findByIdAndOwnerKey(ChatSessionId.of(sessionId), ownerKey)
            .map(this::withStoredMessages)
            .orElseThrow(() -> new ChatSessionNotFoundException(sessionId));
    return new SessionHistory(
        session.getMessages(), chatWebSourcesRepository.findByConversationId(sessionId));
  }

  /** Sends a message in the session and returns the reply. */
  public String chatWithSession(String sessionId, String userMessage, String ownerKey) {
    ChatSession session = loadOrCreateSession(sessionId, ownerKey);
    return exchangeMessages(session, sessionId, userMessage, TextChatOptions.defaults());
  }

  /** Sends a message in the owner's default session and returns the reply. */
  public String chatWithSession(String userMessage, String ownerKey) {
    ChatSession session = getOrCreateDefaultSession(ownerKey);
    return exchangeMessages(
        session, session.getId().value(), userMessage, TextChatOptions.defaults());
  }

  /** Creates an empty chat session owned by the client. */
  public ChatSession createSession(String title, String ownerKey) {
    ChatSession session = ChatSession.create(title, ownerKey);
    repository.save(session);
    log.info(
        "Created new session titleLength={} idFp={} clientFp={}",
        LogSanitizer.lengthOf(session.getTitle()),
        LogSanitizer.fingerprint(session.getId().value()),
        LogSanitizer.fingerprint(ownerKey));
    return session;
  }

  /** Deletes the client's session and its chat memory. */
  public void deleteSession(String sessionId, String ownerKey) {
    ChatSession session =
        repository
            .findByIdAndOwnerKey(ChatSessionId.of(sessionId), ownerKey)
            .orElseThrow(() -> new ChatSessionNotFoundException(sessionId));
    sessionEraser.eraseAll(List.of(session));
    log.info("Deleted sessionFp={}", LogSanitizer.fingerprint(sessionId));
  }

  /** Streams a reply within a session, persisting both turns after completion. */
  public Flux<String> streamChatWithSession(
      String sessionId, String userMessage, TextChatOptions options, String ownerKey) {
    return Flux.defer(
            () -> {
              long startedAt = System.nanoTime();
              ChatSession session = loadOrCreateSession(sessionId, ownerKey);
              boolean isFirstTurn = session.isEmpty();
              conversationMemoryRepository.seedIfEmpty(sessionId, session.getMessages());

              ToolEventChannel.setCurrentSessionId(sessionId);
              try {
                ChatClient chatClient = chatClientProvider.create(options, sessionId);
                AtomicReference<String> rawAssistant = new AtomicReference<>("");
                Flux<String> primary =
                    mergeToolEvents(
                        chatClient
                            .prompt()
                            .advisors(
                                advisor -> advisor.param(ChatMemory.CONVERSATION_ID, sessionId))
                            .user(userMessage)
                            .stream()
                            .content()
                            .doOnNext(token -> rawAssistant.updateAndGet(prev -> prev + token)),
                        sessionId,
                        ownerKey,
                        options.toolsEnabled());
                Flux<String> repaired =
                    Flux.defer(
                        () -> repairIfToolMarkupOnly(rawAssistant.get(), sessionId, options));
                return Flux.concat(primary, repaired)
                    .doOnComplete(
                        () -> {
                          invocationRecorder.recordSuccess(
                              AiDomain.CHAT,
                              "chat.stream",
                              measureElapsedMs(startedAt),
                              options.provider(),
                              options.model(),
                              sessionId);
                          Mono.fromRunnable(
                                  () ->
                                      finishSessionStream(
                                          session.getId(), sessionId, isFirstTurn, userMessage))
                              .subscribeOn(Schedulers.boundedElastic())
                              .subscribe();
                        })
                    .doOnError(
                        error -> {
                          log.error(
                              "Stream failed for sessionFp={}",
                              LogSanitizer.fingerprint(sessionId),
                              error);
                          invocationRecorder.recordError(
                              AiDomain.CHAT,
                              "chat.stream",
                              measureElapsedMs(startedAt),
                              options.provider(),
                              options.model(),
                              sessionId,
                              error.getClass().getSimpleName(),
                              error.getMessage());
                        });
              } finally {
                ToolEventChannel.clearCurrentSessionId();
              }
            })
        .subscribeOn(Schedulers.boundedElastic());
  }

  /** Streams a reply for an ad-hoc message list without a session. */
  public Flux<String> streamChat(
      List<ChatMessage> messages, TextChatOptions options, String ownerKey) {
    String requestId = java.util.UUID.randomUUID().toString();
    ToolEventChannel.setCurrentSessionId(requestId);
    try {
      ChatClient chatClient = chatClientProvider.createStateless(options);
      return mergeToolEvents(
          chatClient
              .prompt()
              .messages(messages.stream().map(this::toSpringMessage).toList())
              .stream()
              .content(),
          requestId,
          ownerKey,
          options.toolsEnabled());
    } finally {
      ToolEventChannel.clearCurrentSessionId();
    }
  }

  /** Sends one stateless message with default options. */
  public String chat(String userMessage) {
    return chat(userMessage, TextChatOptions.defaults());
  }

  /** Sends one stateless message with retries and records the invocation. */
  public String chat(String userMessage, TextChatOptions options) {
    log.info("Chat request with retry: message length={}", LogSanitizer.lengthOf(userMessage));
    long startedAt = System.nanoTime();
    try {
      String response =
          retryTemplate.execute(
              context -> {
                ChatClient chatClient = chatClientProvider.createStateless(options);
                String content = chatClient.prompt().user(userMessage).call().content();
                if (content == null || content.isBlank()) {
                  throw new AiServiceException("AI returned empty response");
                }
                return content;
              });
      invocationRecorder.recordSuccess(
          AiDomain.CHAT,
          "chat.call",
          measureElapsedMs(startedAt),
          options.provider(),
          options.model(),
          null);
      return response;
    } catch (RuntimeException ex) {
      invocationRecorder.recordError(
          AiDomain.CHAT,
          "chat.call",
          measureElapsedMs(startedAt),
          options.provider(),
          options.model(),
          null,
          ex.getClass().getSimpleName(),
          ex.getMessage());
      throw ex;
    }
  }

  /** Returns the session when it belongs to the client. */
  public Optional<ChatSession> getSession(String sessionId, String ownerKey) {
    return repository
        .findByIdAndOwnerKey(ChatSessionId.of(sessionId), ownerKey)
        .map(this::withStoredMessages);
  }

  /** Deletes every session owned by the client. */
  public void deleteAllSessions(String ownerKey) {
    List<ChatSession> sessions = repository.findByOwnerKey(ownerKey);
    int metricsDeleted = sessionEraser.eraseAll(sessions);
    log.info(
        "Erased {} sessions and {} metrics events for clientFp={}",
        sessions.size(),
        metricsDeleted,
        LogSanitizer.fingerprint(ownerKey));
  }

  /** Clears the model memory of a conversation. */
  public void clearConversationMemory(String conversationId) {
    chatMemory.clear(conversationId);
  }

  private static long measureElapsedMs(long startedAtNanos) {
    return (System.nanoTime() - startedAtNanos) / 1_000_000L;
  }

  private Flux<String> repairIfToolMarkupOnly(
      String rawAssistant, String sessionId, TextChatOptions options) {
    if (!ToolCallMarkupFilter.looksLikeToolMarkup(rawAssistant)) {
      return Flux.empty();
    }
    if (!ToolCallMarkupFilter.sanitize(rawAssistant).isBlank()) {
      return Flux.empty();
    }
    log.warn(
        "Assistant returned tool markup only for sessionFp={}; repairing without tools",
        LogSanitizer.fingerprint(sessionId));
    TextChatOptions noTools = TextChatOptions.of(options.provider(), options.model(), false);
    final ChatClient repairClient = chatClientProvider.createBareStateless(noTools);
    List<Message> promptMessages = new ArrayList<>();
    promptMessages.add(new SystemMessage(promptTemplates.getDefaultSystemPrompt()));
    promptMessages.addAll(chatMemory.get(sessionId));
    CapturedWebSources.Capture capture = CapturedWebSources.peek(sessionId);
    if (capture != null && !capture.sources().isEmpty()) {
      promptMessages.add(new SystemMessage(formatCapturedSources(capture)));
    }
    promptMessages.add(new SystemMessage(promptTemplates.getAfterToolsReminder()));
    promptMessages.add(new UserMessage(REPAIR_USER_PROMPT));

    StringBuilder repaired = new StringBuilder();
    return repairClient.prompt().messages(promptMessages).stream()
        .content()
        .doOnNext(repaired::append)
        .map(this::sanitizeStreamToken)
        .filter(token -> !token.isEmpty())
        .map(StreamTokenEvent::toJson)
        .doOnComplete(
            () -> {
              String text = ToolCallMarkupFilter.sanitize(repaired.toString());
              if (!text.isBlank()) {
                chatMemory.add(sessionId, List.of(new AssistantMessage(text)));
              }
            });
  }

  private static String formatCapturedSources(CapturedWebSources.Capture capture) {
    StringBuilder sb = new StringBuilder();
    sb.append("Web search results already retrieved for query: ")
        .append(capture.query())
        .append("\n\n");
    int index = 1;
    for (var source : capture.sources()) {
      sb.append('[')
          .append(index++)
          .append("] ")
          .append(source.title())
          .append('\n')
          .append("URL: ")
          .append(source.url())
          .append('\n')
          .append("Summary: ")
          .append(source.snippet())
          .append("\n\n");
    }
    sb.append(
        "Use these results for the final answer and chart."
            + " Do not claim search results are missing.");
    return sb.toString();
  }

  private Flux<String> mergeToolEvents(
      Flux<String> content, String channelId, String ownerKey, boolean toolsEnabled) {
    Flux<String> textTokens = content.map(this::sanitizeStreamToken);
    if (!toolsEnabled) {
      return textTokens.filter(token -> !token.isEmpty()).map(StreamTokenEvent::toJson);
    }
    Sinks.Many<String> sink = ToolEventChannel.open(channelId);
    ToolEventChannel.bindOwnerKey(channelId, ownerKey);
    Flux<String> toolEvents =
        ToolEventChannel.asFlux(sink).doOnNext(json -> captureSourcesEvent(channelId, json));
    Flux<String> textEvents =
        textTokens
            .filter(token -> !token.isEmpty())
            .map(StreamTokenEvent::toJson)
            .doFinally(signal -> ToolEventChannel.close(channelId));
    return Flux.merge(toolEvents, textEvents);
  }

  private String sanitizeStreamToken(String token) {
    if (!ToolCallMarkupFilter.looksLikeToolMarkup(token)) {
      return token;
    }
    return ToolCallMarkupFilter.sanitize(token);
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

  private void finishSessionStream(
      ChatSessionId sessionId, String conversationId, boolean isFirstTurn, String userMessage) {
    repository
        .findById(sessionId)
        .ifPresent(
            session -> {
              session.recordExchange(conversationMemoryRepository.load(conversationId));
              repository.save(session);
              persistCapturedSources(conversationId, session);
              if (isFirstTurn && session.needsGeneratedTitle()) {
                generateTitleAsync(
                    sessionId, userMessage, session.lastAssistantMessage().orElseThrow().getText());
              }
            });
  }

  private void persistCapturedSources(String conversationId, ChatSession session) {
    CapturedWebSources.Capture capture = CapturedWebSources.take(conversationId);
    if (capture == null || capture.sources().isEmpty()) {
      return;
    }
    session
        .lastAssistantMessage()
        .ifPresentOrElse(
            reply ->
                chatWebSourcesRepository.save(
                    conversationId, reply.getText(), capture.query(), capture.sources()),
            () -> CapturedWebSources.clear(conversationId));
  }

  private String exchangeMessages(
      ChatSession session, String conversationId, String userMessage, TextChatOptions options) {
    conversationMemoryRepository.seedIfEmpty(conversationId, session.getMessages());
    final boolean isFirstTurn = session.isEmpty();

    ChatClient chatClient = chatClientProvider.create(options, conversationId);
    String aiResponse =
        chatClient
            .prompt()
            .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
            .user(userMessage)
            .call()
            .content();

    if (aiResponse == null || aiResponse.isBlank()) {
      throw new AiServiceException("AI returned empty response");
    }

    session.recordExchange(conversationMemoryRepository.load(conversationId));
    repository.save(session);

    if (isFirstTurn && session.needsGeneratedTitle()) {
      generateTitleAsync(session.getId(), userMessage, aiResponse);
    }

    return aiResponse;
  }

  private void generateTitleAsync(
      ChatSessionId sessionId, String userMessage, String assistantReply) {
    Mono.fromCallable(() -> sessionTitleGenerator.generate(userMessage, assistantReply))
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe(
            title ->
                repository
                    .findById(sessionId)
                    .ifPresent(
                        session -> {
                          if (session.applyGeneratedTitle(title)) {
                            repository.save(session);
                            log.info(
                                "Renamed sessionFp={} titleLength={}",
                                LogSanitizer.fingerprint(sessionId.value()),
                                LogSanitizer.lengthOf(title.value()));
                          }
                        }),
            error ->
                log.warn(
                    "Async title generation failed for sessionFp={}",
                    LogSanitizer.fingerprint(sessionId.value()),
                    error));
  }

  private ChatSession withStoredMessages(ChatSession session) {
    session.restoreMessages(conversationMemoryRepository.load(session.getId().value()));
    return session;
  }

  private ChatSession getOrCreateDefaultSession(String ownerKey) {
    List<ChatSession> sessions = repository.findByOwnerKey(ownerKey);
    if (sessions.isEmpty()) {
      ChatSession newSession = ChatSession.startDefault(ownerKey);
      repository.save(newSession);
      return newSession;
    }
    return sessions.getFirst();
  }

  private ChatSession loadOrCreateSession(String sessionId, String ownerKey) {
    ChatSessionId id = ChatSessionId.of(sessionId);
    Optional<ChatSession> owned = repository.findByIdAndOwnerKey(id, ownerKey);
    if (owned.isPresent()) {
      return owned.get();
    }
    if (repository.exists(id)) {
      throw new ChatSessionNotFoundException(sessionId);
    }
    ChatSession session = ChatSession.startWithId(id, ownerKey);
    repository.save(session);
    return session;
  }

  private Message toSpringMessage(ChatMessage msg) {
    return msg.isFromUser() ? new UserMessage(msg.getText()) : new AssistantMessage(msg.getText());
  }
}
