package com.ai.rag.service;

import com.ai.chat.domain.service.LanguageDetectionService;
import com.ai.common.infra.logging.LogSanitizer;
import com.ai.common.service.llm.ChatClientProfile;
import com.ai.common.service.llm.ChatClientProvider;
import com.ai.common.service.llm.StreamTokenEvent;
import com.ai.common.service.llm.TextChatOptions;
import com.ai.metrics.domain.model.AiInvocationEvent;
import com.ai.metrics.domain.vo.AiDomain;
import com.ai.metrics.domain.vo.InvocationOutcome;
import com.ai.metrics.service.AiInvocationRecorder;
import com.ai.rag.domain.model.ChunkMetadataKeys;
import com.ai.rag.domain.model.SourceDocument;
import com.ai.rag.domain.repository.RagRetrievalSettings;
import com.ai.rag.service.dto.RagChatResult;
import com.ai.rag.service.dto.RagSourceEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
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
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/** RAG chat over uploaded documents via Spring AI's retrieval augmentation advisor. */
@Service
@RequiredArgsConstructor
public class RagChatService {

  private static final Logger log = LoggerFactory.getLogger(RagChatService.class);

  private final ChatClientProvider chatClientProvider;
  private final LanguageDetectionService languageDetectionService;
  private final VectorStore vectorStore;
  private final RagRetrievalSettings retrievalSettings;
  private final AiInvocationRecorder invocationRecorder;
  private final ObjectMapper objectMapper;

  /** True token streaming via ChatClient; emits {@code sources} SSE after content completes. */
  public Flux<ServerSentEvent<String>> streamChat(
      String question, List<String> documentIds, int topK, String sessionId, String ownerKey) {
    long startedAt = System.nanoTime();
    TextChatOptions options = TextChatOptions.withoutTools();
    String documentId =
        documentIds != null && !documentIds.isEmpty() ? documentIds.getFirst() : null;
    AtomicReference<List<SourceDocument>> sourcesRef = new AtomicReference<>(List.of());

    ChatClient.ChatClientRequestSpec promptSpec;
    try {
      promptSpec = buildPrompt(question, documentIds, topK, sessionId, ownerKey, options);
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
              return ServerSentEvent.<String>builder().data(StreamTokenEvent.toJson(piece)).build();
            })
        .concatWith(Flux.defer(() -> buildSourceEvents(sourcesRef.get())))
        .doOnComplete(() -> recordSuccess(options, sessionId, documentId, startedAt))
        .doOnError(ex -> recordError(sessionId, startedAt, ex));
  }

  /**
   * Answers the question with context retrieved from the owner's documents and records the
   * invocation.
   */
  public RagChatResult chat(
      String question, List<String> documentIds, int topK, String sessionId, String ownerKey) {
    long startedAt = System.nanoTime();
    TextChatOptions options = TextChatOptions.withoutTools();
    String documentId =
        documentIds != null && !documentIds.isEmpty() ? documentIds.getFirst() : null;
    try {
      ChatClient.ChatClientRequestSpec promptSpec =
          buildPrompt(question, documentIds, topK, sessionId, ownerKey, options);
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

  private ChatClient.ChatClientRequestSpec buildPrompt(
      String question,
      List<String> documentIds,
      int topK,
      String sessionId,
      String ownerKey,
      TextChatOptions options) {
    log.info("RAG chat request: question length={}", LogSanitizer.lengthOf(question));
    Filter.Expression filter = buildRetrievalFilter(ownerKey, documentIds);

    String languageCode = languageDetectionService.detect(question);
    String languageHint =
        "Respond in the same language as the user question (detected: " + languageCode + ").";

    boolean withMemory = sessionId != null && !sessionId.isBlank();
    ChatClientProfile profile = withMemory ? ChatClientProfile.MEMORY : ChatClientProfile.BARE;
    ChatClient chatClient = chatClientProvider.create(options, profile, sessionId);

    VectorStoreDocumentRetriever documentRetriever =
        VectorStoreDocumentRetriever.builder()
            .vectorStore(vectorStore)
            .topK(topK)
            .similarityThreshold(retrievalSettings.getScoreThreshold())
            .build();

    var advisorBuilder =
        RetrievalAugmentationAdvisor.builder().documentRetriever(documentRetriever);
    if (withMemory) {
      // The memory advisor rejects calls without a conversation id, which this call never has.
      ChatClient compressionClient = chatClientProvider.createBareStateless(options);
      advisorBuilder.queryTransformers(
          CompressionQueryTransformer.builder()
              .chatClientBuilder(compressionClient.mutate())
              .build());
    }

    var promptSpec =
        chatClient
            .prompt()
            .advisors(advisorBuilder.build())
            .advisors(
                a -> {
                  a.param(VectorStoreDocumentRetriever.FILTER_EXPRESSION, filter);
                  if (withMemory) {
                    a.param(ChatMemory.CONVERSATION_ID, sessionId);
                  }
                })
            .system(languageHint)
            .user(question);
    return promptSpec;
  }

  private static Filter.Expression buildRetrievalFilter(String ownerKey, List<String> documentIds) {
    FilterExpressionBuilder builder = new FilterExpressionBuilder();
    FilterExpressionBuilder.Op ownedByCaller = builder.eq(ChunkMetadataKeys.OWNER_KEY, ownerKey);
    if (documentIds == null || documentIds.isEmpty()) {
      return ownedByCaller.build();
    }
    List<Object> ids = List.copyOf(documentIds);
    return builder.and(ownedByCaller, builder.in(ChunkMetadataKeys.DOCUMENT_ID, ids)).build();
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

  private void recordError(String sessionId, long startedAt, Throwable ex) {
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

  private Flux<ServerSentEvent<String>> buildSourceEvents(List<SourceDocument> sources) {
    if (sources.isEmpty()) {
      return Flux.empty();
    }
    try {
      List<RagSourceEvent> payload = RagSourceEvent.fromAll(sources);
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
        .map(RagChatService::toSourceDocument)
        .toList();
  }

  private static SourceDocument toSourceDocument(Document document) {
    Map<String, Object> metadata = document.getMetadata();
    double score = 0.0;
    Object scoreMeta = metadata.get("score");
    if (scoreMeta instanceof Number number) {
      score = number.doubleValue();
    } else if (document.getScore() != null) {
      score = document.getScore();
    }
    return new SourceDocument(
        document.getText() != null ? document.getText() : "", score, metadata);
  }
}
