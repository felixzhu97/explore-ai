package com.ai.chat.application.usecase;

import com.ai.chat.domain.exception.ChatSessionNotFoundException;
import com.ai.chat.domain.model.ChatMessage;
import com.ai.chat.domain.model.ChatSession;
import com.ai.chat.domain.repository.ChatSessionRepository;
import com.ai.chat.domain.repository.ChatWebSourcesRepository;
import com.ai.chat.domain.repository.ConversationMemoryRepository;
import com.ai.chat.domain.vo.ChatSessionId;
import com.ai.common.application.llm.ChatClientProvider;
import com.ai.common.application.llm.TextChatOptions;
import com.ai.common.domain.exception.AiServiceException;
import com.ai.common.infrastructure.llm.ToolCallMarkupFilter;
import com.ai.common.infrastructure.llm.ToolEventChannel;
import com.ai.common.infrastructure.prompt.PromptTemplates;
import com.ai.common.util.LogSanitizer;
import com.ai.metrics.application.AiInvocationRecorder;
import com.ai.metrics.domain.repository.AiInvocationEventRepository;
import com.ai.metrics.domain.vo.AiDomain;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
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

/** Documentation. */
@Service
public class SpringAiChatUseCase implements ChatUseCase {

  private static final Logger log = LoggerFactory.getLogger(SpringAiChatUseCase.class);
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
  private final AiInvocationEventRepository invocationEventRepository;

  /** Documentation. */
  public SpringAiChatUseCase(
      ChatClientProvider chatClientProvider,
      ChatSessionRepository repository,
      RetryTemplate retryTemplate,
      ChatMemory chatMemory,
      ConversationMemoryRepository conversationMemoryRepository,
      SessionTitleGenerator sessionTitleGenerator,
      ChatWebSourcesRepository chatWebSourcesRepository,
      PromptTemplates promptTemplates,
      AiInvocationRecorder invocationRecorder,
      AiInvocationEventRepository invocationEventRepository) {
    this.chatClientProvider = chatClientProvider;
    this.repository = repository;
    this.retryTemplate = retryTemplate;
    this.chatMemory = chatMemory;
    this.conversationMemoryRepository = conversationMemoryRepository;
    this.sessionTitleGenerator = sessionTitleGenerator;
    this.chatWebSourcesRepository = chatWebSourcesRepository;
    this.promptTemplates = promptTemplates;
    this.invocationRecorder = invocationRecorder;
    this.invocationEventRepository = invocationEventRepository;
  }

  @Override
  public String chat(String userMessage) {
    return chat(userMessage, TextChatOptions.defaults());
  }

  @Override
  public String chat(String userMessage, TextChatOptions options) {
    log.info("Chat request with retry: {}", LogSanitizer.truncate(userMessage, 100));
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
          elapsedMs(startedAt),
          options.provider(),
          options.model(),
          null);
      return response;
    } catch (RuntimeException ex) {
      invocationRecorder.recordError(
          AiDomain.CHAT,
          "chat.call",
          elapsedMs(startedAt),
          options.provider(),
          options.model(),
          null,
          ex.getClass().getSimpleName(),
          ex.getMessage());
      throw ex;
    }
  }

  @Override
  public Flux<String> chatStreamWithSession(String sessionId, String userMessage, String clientId) {
    return chatStreamWithSession(sessionId, userMessage, TextChatOptions.defaults(), clientId);
  }

  @Override
  public Flux<String> chatStreamWithSession(
      String sessionId, String userMessage, TextChatOptions options, String clientId) {
    return Flux.defer(
            () -> {
              long startedAt = System.nanoTime();
              ChatSession session = loadOrCreateSession(sessionId, clientId);
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
                            .doOnNext(
                                token -> {
                                  if (token != null && !token.isEmpty()) {
                                    rawAssistant.updateAndGet(prev -> prev + token);
                                  }
                                }),
                        sessionId,
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
                              elapsedMs(startedAt),
                              options.provider(),
                              options.model(),
                              sessionId);
                          Mono.fromRunnable(
                                  () ->
                                      afterSessionStream(
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
                              elapsedMs(startedAt),
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

  private static long elapsedMs(long startedAtNanos) {
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
        .doOnNext(
            token -> {
              if (token != null) {
                repaired.append(token);
              }
            })
        .map(this::sanitizeStreamToken)
        .filter(token -> !token.isEmpty())
        .map(this::messageEvent)
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
      Flux<String> content, String channelId, boolean toolsEnabled) {
    Flux<String> textTokens = content.map(this::sanitizeStreamToken);
    if (!toolsEnabled) {
      return textTokens.filter(token -> !token.isEmpty()).map(this::messageEvent);
    }
    Sinks.Many<String> sink = ToolEventChannel.open(channelId);
    Flux<String> toolEvents =
        ToolEventChannel.asFlux(sink).doOnNext(json -> captureSourcesEvent(channelId, json));
    Flux<String> textEvents =
        textTokens
            .filter(token -> !token.isEmpty())
            .map(this::messageEvent)
            .doFinally(signal -> ToolEventChannel.close(channelId));
    return Flux.merge(toolEvents, textEvents);
  }

  private String sanitizeStreamToken(String token) {
    if (token == null || token.isEmpty()) {
      return "";
    }
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

  private String messageEvent(String token) {
    try {
      return JSON.writeValueAsString(
          Map.of("type", "message", "token", token == null ? "" : token));
    } catch (JsonProcessingException e) {
      return "{\"type\":\"message\",\"token\":\"\"}";
    }
  }

  private void afterSessionStream(
      ChatSessionId sessionId, String conversationId, boolean isFirstTurn, String userMessage) {
    repository
        .findById(sessionId)
        .ifPresent(
            session -> {
              conversationMemoryRepository.syncToSession(conversationId, session);
              repository.save(session);
              persistCapturedSources(conversationId, session);
              if (isFirstTurn && session.hasDefaultTitle()) {
                String assistantReply =
                    session.getLastAssistantMessage() != null
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
        conversationId, lastAssistant.getText(), capture.query(), capture.sources());
  }

  @Override
  public String chatWithSession(String sessionId, String userMessage, String clientId) {
    ChatSession session = loadOrCreateSession(sessionId, clientId);
    return exchangeMessages(session, sessionId, userMessage, TextChatOptions.defaults());
  }

  @Override
  public String chatWithSession(String userMessage, String clientId) {
    ChatSession session = getOrCreateDefaultSession(clientId);
    return exchangeMessages(
        session, session.getId().value(), userMessage, TextChatOptions.defaults());
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
          chatClient
              .prompt()
              .messages(messages.stream().map(this::toSpringMessage).toList())
              .stream()
              .content(),
          requestId,
          options.toolsEnabled());
    } finally {
      ToolEventChannel.clearCurrentSessionId();
    }
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
                          if (session.hasDefaultTitle()) {
                            session.rename(title);
                            repository.save(session);
                            log.info(
                                "Renamed sessionFp={} title={}",
                                LogSanitizer.fingerprint(sessionId.value()),
                                title);
                          }
                        }),
            error ->
                log.warn(
                    "Async title generation failed for sessionFp={}",
                    LogSanitizer.fingerprint(sessionId.value()),
                    error));
  }

  private ChatSession getOrCreateDefaultSession(String clientId) {
    List<ChatSession> sessions = repository.findByClientId(clientId);
    if (sessions.isEmpty()) {
      ChatSession newSession = ChatSession.create("Default Chat", clientId);
      repository.save(newSession);
      return newSession;
    }
    return sessions.getFirst();
  }

  private ChatSession loadOrCreateSession(String sessionId, String clientId) {
    ChatSessionId id = ChatSessionId.of(sessionId);
    Optional<ChatSession> owned = repository.findByIdAndClientId(id, clientId);
    if (owned.isPresent()) {
      return owned.get();
    }
    if (repository.exists(id)) {
      throw new ChatSessionNotFoundException(sessionId);
    }
    ChatSession session = ChatSession.createWithId(id, ChatSession.DEFAULT_TITLE, clientId);
    repository.save(session);
    return session;
  }

  @Override
  public ChatSession createSession(String title, String clientId) {
    ChatSession session = ChatSession.create(title, clientId);
    repository.save(session);
    log.info(
        "Created new session title={} idFp={} clientFp={}",
        title,
        LogSanitizer.fingerprint(session.getId().value()),
        LogSanitizer.fingerprint(clientId));
    return session;
  }

  @Override
  public Optional<ChatSession> getSession(String sessionId, String clientId) {
    return repository
        .findByIdAndClientId(ChatSessionId.of(sessionId), clientId)
        .map(
            session -> {
              conversationMemoryRepository.syncToSession(sessionId, session);
              return session;
            });
  }

  @Override
  public List<ChatMessage> getSessionHistory(String sessionId, String clientId) {
    ChatSession session =
        repository
            .findByIdAndClientId(ChatSessionId.of(sessionId), clientId)
            .orElseThrow(() -> new ChatSessionNotFoundException(sessionId));
    conversationMemoryRepository.syncToSession(sessionId, session);
    return session.getMessages();
  }

  @Override
  public void deleteSession(String sessionId, String clientId) {
    ChatSessionId id = ChatSessionId.of(sessionId);
    if (repository.findByIdAndClientId(id, clientId).isEmpty()) {
      throw new ChatSessionNotFoundException(sessionId);
    }
    conversationMemoryRepository.clear(sessionId);
    chatWebSourcesRepository.deleteByConversationId(sessionId);
    CapturedWebSources.clear(sessionId);
    repository.delete(id);
    log.info("Deleted sessionFp={}", LogSanitizer.fingerprint(sessionId));
  }

  @Override
  public void deleteAllSessionsForClient(String clientId) {
    List<ChatSession> sessions = repository.findByClientId(clientId);
    List<String> sessionIds = sessions.stream().map(session -> session.getId().value()).toList();
    for (ChatSession session : sessions) {
      String sessionId = session.getId().value();
      conversationMemoryRepository.clear(sessionId);
      chatWebSourcesRepository.deleteByConversationId(sessionId);
      CapturedWebSources.clear(sessionId);
      repository.delete(session.getId());
    }
    int metricsDeleted = invocationEventRepository.deleteBySessionIds(sessionIds);
    log.info(
        "Erased {} sessions and {} metrics events for clientFp={}",
        sessions.size(),
        metricsDeleted,
        LogSanitizer.fingerprint(clientId));
  }

  @Override
  public List<ChatSession> getSessionsForClient(String clientId) {
    return repository.findByClientId(clientId).stream()
        .map(
            session -> {
              conversationMemoryRepository.syncToSession(session.getId().value(), session);
              return session;
            })
        .toList();
  }

  /** Documentation. */
  public void clearConversationMemory(String conversationId) {
    chatMemory.clear(conversationId);
  }

  private Message toSpringMessage(ChatMessage msg) {
    return msg.isFromUser() ? new UserMessage(msg.getText()) : new AssistantMessage(msg.getText());
  }
}
