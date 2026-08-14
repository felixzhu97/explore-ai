package com.ai.rag.application.usecase;

import com.ai.chat.domain.service.LanguageDetectionService;
import com.ai.common.application.llm.ChatClientProfile;
import com.ai.common.application.llm.ChatClientProvider;
import com.ai.common.application.llm.TextChatOptions;
import com.ai.common.util.LogSanitizer;
import com.ai.metrics.application.AiInvocationRecorder;
import com.ai.metrics.domain.model.AiInvocationEvent;
import com.ai.metrics.domain.vo.AiDomain;
import com.ai.metrics.domain.vo.InvocationOutcome;
import com.ai.rag.application.dto.RagChatResult;
import com.ai.rag.domain.model.SourceDocument;
import com.ai.rag.domain.repository.RagRetrievalSettings;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/** Documentation. */
@Service
public class RagChatUseCase {

  private static final Logger log = LoggerFactory.getLogger(RagChatUseCase.class);
  private static final int DEFAULT_TOP_K = 5;

  /** Must match {@code H2SpringAiVectorStore.DOCUMENT_ID_METADATA_KEY}. */
  private static final String DOCUMENT_ID_METADATA_KEY = "document_id";

  private final ChatClientProvider chatClientProvider;
  private final LanguageDetectionService languageDetectionService;
  private final VectorStore vectorStore;
  private final RagRetrievalSettings retrievalSettings;
  private final AiInvocationRecorder invocationRecorder;
  private final ObjectMapper objectMapper;

  /** Documentation. */
  public RagChatUseCase(
      ChatClientProvider chatClientProvider,
      LanguageDetectionService languageDetectionService,
      VectorStore vectorStore,
      RagRetrievalSettings retrievalSettings,
      AiInvocationRecorder invocationRecorder,
      ObjectMapper objectMapper) {
    this.chatClientProvider = chatClientProvider;
    this.languageDetectionService = languageDetectionService;
    this.vectorStore = vectorStore;
    this.retrievalSettings = retrievalSettings;
    this.invocationRecorder = invocationRecorder;
    this.objectMapper = objectMapper;
  }

  /** Documentation. */
  public RagChatResult chat(String question, List<String> docIds, Integer topK) {
    return chat(question, docIds, topK, null);
  }

  /** Documentation. */
  public RagChatResult chat(String question, List<String> docIds, Integer topK, String sessionId) {
    long startedAt = System.nanoTime();
    TextChatOptions options = TextChatOptions.withoutTools();
    String documentId = docIds != null && !docIds.isEmpty() ? docIds.getFirst() : null;
    try {
      ChatClient.ChatClientRequestSpec promptSpec =
          buildPrompt(question, docIds, topK, sessionId, options);
      ChatClientResponse clientResponse = promptSpec.call().chatClientResponse();
      String aiResponse = extractContent(clientResponse);
      List<SourceDocument> sources = extractSources(clientResponse);
      recordSuccess(options, sessionId, documentId, startedAt);
      return new RagChatResult(aiResponse, sources);
    } catch (RuntimeException ex) {
      recordError(sessionId, startedAt, ex);
      throw ex;
    }
  }

  /** True token streaming via ChatClient; emits {@code sources} SSE after content completes. */
  public Flux<ServerSentEvent<String>> chatStream(
      String question, List<String> docIds, Integer topK, String sessionId) {
    long startedAt = System.nanoTime();
    TextChatOptions options = TextChatOptions.withoutTools();
    String documentId = docIds != null && !docIds.isEmpty() ? docIds.getFirst() : null;
    AtomicReference<List<SourceDocument>> sourcesRef = new AtomicReference<>(List.of());

    ChatClient.ChatClientRequestSpec promptSpec;
    try {
      promptSpec = buildPrompt(question, docIds, topK, sessionId, options);
    } catch (RuntimeException ex) {
      recordError(sessionId, startedAt, ex);
      return Flux.error(ex);
    }

    return promptSpec.stream()
        .chatClientResponse()
        .mapNotNull(
            response -> {
              List<SourceDocument> sources = extractSources(response);
              if (!sources.isEmpty()) {
                sourcesRef.set(sources);
              }
              String piece = extractContent(response);
              if (piece.isEmpty()) {
                return null;
              }
              return ServerSentEvent.<String>builder().data(piece).build();
            })
        .concatWith(Flux.defer(() -> sourceEvents(sourcesRef.get())))
        .doOnComplete(() -> recordSuccess(options, sessionId, documentId, startedAt))
        .doOnError(
            ex ->
                recordError(
                    sessionId,
                    startedAt,
                    ex instanceof RuntimeException runtimeException
                        ? runtimeException
                        : new RuntimeException(ex)));
  }

  private ChatClient.ChatClientRequestSpec buildPrompt(
      String question,
      List<String> docIds,
      Integer topK,
      String sessionId,
      TextChatOptions options) {
    log.info("RAG chat request: {}", LogSanitizer.truncate(question));
    int topKValue = topK != null ? topK : DEFAULT_TOP_K;
    List<String> filterDocIds = docIds != null && !docIds.isEmpty() ? List.copyOf(docIds) : null;

    String languageCode = languageDetectionService.detect(question);
    String languageHint =
        "Respond in the same language as the user question (detected: " + languageCode + ").";

    boolean withMemory = sessionId != null && !sessionId.isBlank();
    ChatClientProfile profile = withMemory ? ChatClientProfile.MEMORY : ChatClientProfile.BARE;
    ChatClient chatClient = chatClientProvider.create(options, profile, sessionId);

    VectorStoreDocumentRetriever documentRetriever =
        VectorStoreDocumentRetriever.builder()
            .vectorStore(vectorStore)
            .topK(topKValue)
            .similarityThreshold(retrievalSettings.getScoreThreshold())
            .build();

    var advisorBuilder =
        RetrievalAugmentationAdvisor.builder().documentRetriever(documentRetriever);
    if (withMemory) {
      advisorBuilder.queryTransformers(
          CompressionQueryTransformer.builder().chatClientBuilder(chatClient.mutate()).build());
    }

    var promptSpec =
        chatClient
            .prompt()
            .advisors(advisorBuilder.build())
            .advisors(
                a -> {
                  if (filterDocIds != null) {
                    List<Object> ids = new ArrayList<>(filterDocIds);
                    a.param(
                        VectorStoreDocumentRetriever.FILTER_EXPRESSION,
                        new FilterExpressionBuilder().in(DOCUMENT_ID_METADATA_KEY, ids).build());
                  }
                  if (withMemory) {
                    a.param(ChatMemory.CONVERSATION_ID, sessionId);
                  }
                })
            .system(languageHint)
            .user(question);
    return promptSpec;
  }

  private void recordSuccess(
      TextChatOptions options, String sessionId, String documentId, long startedAt) {
    invocationRecorder.record(
        AiInvocationEvent.builder()
            .domain(AiDomain.RAG)
            .operation("rag.chat")
            .outcome(InvocationOutcome.SUCCESS)
            .latencyMs((System.nanoTime() - startedAt) / 1_000_000L)
            .provider(options.provider())
            .model(options.model())
            .sessionId(sessionId)
            .documentId(documentId)
            .build());
    log.info("RAG chat completed successfully");
  }

  private void recordError(String sessionId, long startedAt, RuntimeException ex) {
    invocationRecorder.recordError(
        AiDomain.RAG,
        "rag.chat",
        (System.nanoTime() - startedAt) / 1_000_000L,
        "openai",
        null,
        sessionId,
        ex.getClass().getSimpleName(),
        ex.getMessage());
  }

  private Flux<ServerSentEvent<String>> sourceEvents(List<SourceDocument> sources) {
    if (sources == null || sources.isEmpty()) {
      return Flux.empty();
    }
    try {
      List<Map<String, Object>> payload = new ArrayList<>();
      for (SourceDocument source : sources) {
        if (source.text() == null || source.text().isBlank()) {
          continue;
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", null);
        row.put("text", source.text());
        row.put("score", source.score());
        row.put("metadata", source.metadata() != null ? source.metadata() : Map.of());
        payload.add(row);
      }
      if (payload.isEmpty()) {
        return Flux.empty();
      }
      String json = objectMapper.writeValueAsString(payload);
      return Flux.just(ServerSentEvent.<String>builder().event("sources").data(json).build());
    } catch (Exception ex) {
      log.warn("Failed to serialize RAG sources for SSE", ex);
      return Flux.empty();
    }
  }

  private static String extractContent(ChatClientResponse clientResponse) {
    ChatResponse chatResponse = clientResponse.chatResponse();
    if (chatResponse == null) {
      return "";
    }
    Generation generation = chatResponse.getResult();
    if (generation == null) {
      return "";
    }
    AssistantMessage output = generation.getOutput();
    return output != null && output.getText() != null ? output.getText() : "";
  }

  private static List<SourceDocument> extractSources(ChatClientResponse clientResponse) {
    Object raw = clientResponse.context().get(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT);
    if (!(raw instanceof List<?> documents) || documents.isEmpty()) {
      return List.of();
    }
    return documents.stream()
        .filter(Document.class::isInstance)
        .map(Document.class::cast)
        .map(RagChatUseCase::toSourceDocument)
        .toList();
  }

  private static SourceDocument toSourceDocument(Document document) {
    Map<String, Object> metadata =
        document.getMetadata() != null ? new HashMap<>(document.getMetadata()) : new HashMap<>();
    double score = 0.0;
    Object scoreMeta = metadata.get("score");
    if (scoreMeta instanceof Number number) {
      score = number.doubleValue();
    } else if (document.getScore() != null) {
      score = document.getScore();
    }
    return new SourceDocument(
        document.getText() != null ? document.getText() : "",
        score,
        Collections.unmodifiableMap(metadata));
  }
}
