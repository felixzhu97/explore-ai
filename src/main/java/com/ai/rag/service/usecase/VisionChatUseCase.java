package com.ai.rag.service.usecase;

import com.ai.chat.infra.prompt.LocalizedRagPromptBuilder;
import com.ai.common.service.llm.ChatClientProvider;
import com.ai.common.service.llm.TextChatOptions;
import com.ai.common.util.LogSanitizer;
import com.ai.rag.domain.model.SourceDocument;
import com.ai.rag.domain.vo.DocumentId;
import com.ai.rag.service.dto.RagChatResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/** Documentation. */
@Service
@ConditionalOnProperty(
    name = "spring.ai.ollama.chat.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class VisionChatUseCase {

  private static final Logger log = LoggerFactory.getLogger(VisionChatUseCase.class);
  private static final int DEFAULT_TOP_K = 5;

  @Value("${spring.ai.ollama.chat.model:qwen3.5:35b}")
  private String visionModel;

  private final RagApplicationService ragApplicationService;
  private final ChatClientProvider chatClientProvider;
  private final LocalizedRagPromptBuilder localizedRagPromptBuilder;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  /** Documentation. */
  public VisionChatUseCase(
      RagApplicationService ragApplicationService,
      ChatClientProvider chatClientProvider,
      LocalizedRagPromptBuilder localizedRagPromptBuilder,
      ObjectMapper objectMapper) {
    this.ragApplicationService = ragApplicationService;
    this.chatClientProvider = chatClientProvider;
    this.localizedRagPromptBuilder = localizedRagPromptBuilder;
    this.objectMapper = objectMapper;
  }

  /** Documentation. */
  public RagChatResult chatWithImages(
      String question, List<String> docIds, List<String> images, Integer topK) {
    log.info(
        "Vision RAG chat request: {} with {} images",
        LogSanitizer.truncate(question),
        images != null ? images.size() : 0);

    List<Media> mediaList = parseImages(images);
    List<DocumentId> docIdList = toDocumentIds(docIds);
    int topKValue = topK != null ? topK : DEFAULT_TOP_K;
    var retrievalResult = ragApplicationService.retrieveContext(question, docIdList, topKValue);

    String prompt = buildPrompt(question, retrievalResult.context());
    String aiResponse = chatWithVision(prompt, mediaList);

    log.info("Vision RAG chat completed successfully");
    return new RagChatResult(aiResponse, retrievalResult.sources());
  }

  /** True token streaming via ChatClient; emits {@code sources} SSE after content completes. */
  public Flux<ServerSentEvent<String>> chatStreamWithImages(
      String question, List<String> docIds, List<String> images, Integer topK) {
    log.info(
        "Vision RAG stream request: {} with {} images",
        LogSanitizer.truncate(question),
        images != null ? images.size() : 0);

    List<Media> mediaList = parseImages(images);
    List<DocumentId> docIdList = toDocumentIds(docIds);
    int topKValue = topK != null ? topK : DEFAULT_TOP_K;
    var retrievalResult = ragApplicationService.retrieveContext(question, docIdList, topKValue);
    String prompt = buildPrompt(question, retrievalResult.context());
    List<SourceDocument> sources = retrievalResult.sources();

    return streamVision(prompt, mediaList)
        .concatWith(Flux.defer(() -> sourceEvents(sources)))
        .doOnComplete(() -> log.info("Vision RAG stream completed successfully"));
  }

  private List<DocumentId> toDocumentIds(List<String> docIds) {
    if (docIds == null || docIds.isEmpty()) {
      return null;
    }
    return docIds.stream().map(DocumentId::of).toList();
  }

  private Flux<ServerSentEvent<String>> streamVision(String prompt, List<Media> images) {
    log.info("Streaming {} images with Ollama vision model: {}", images.size(), visionModel);
    try {
      ChatClient chatClient =
          chatClientProvider.createStateless(TextChatOptions.ollamaVision(visionModel));

      return chatClient
          .prompt()
          .user(user -> user.text(prompt).media(images.toArray(Media[]::new)))
          .stream()
          .content()
          .filter(piece -> piece != null && !piece.isEmpty())
          .map(piece -> ServerSentEvent.<String>builder().data(piece).build())
          .onErrorResume(
              ex -> {
                log.error("Error in vision stream: {}", ex.getMessage(), ex);
                return Flux.just(errorEvent("Error processing images: " + ex.getMessage()));
              });
    } catch (RuntimeException ex) {
      log.error("Error starting vision stream: {}", ex.getMessage(), ex);
      return Flux.just(errorEvent("Error processing images: " + ex.getMessage()));
    }
  }

  private static ServerSentEvent<String> errorEvent(String message) {
    return ServerSentEvent.<String>builder().event("error").data(message).build();
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
      log.warn("Failed to serialize vision RAG sources for SSE", ex);
      return Flux.empty();
    }
  }

  private List<Media> parseImages(List<String> images) {
    if (images == null || images.isEmpty()) {
      return List.of();
    }

    return images.stream()
        .filter(img -> img != null && !img.isBlank())
        .map(this::parseImage)
        .filter(m -> m != null)
        .toList();
  }

  private Media parseImage(String imageData) {
    String trimmed = imageData.trim();

    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
      try {
        byte[] imageBytes =
            httpClient
                .send(
                    HttpRequest.newBuilder().uri(URI.create(trimmed)).GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray())
                .body();

        String base64 = java.util.Base64.getEncoder().encodeToString(imageBytes);
        return Media.builder().mimeType(MediaType.IMAGE_PNG).data(base64).build();
      } catch (Exception e) {
        log.warn("Failed to fetch image from URL {}: {}", trimmed, e.getMessage());
        return null;
      }
    }

    if (trimmed.startsWith("data:image/")) {
      int commaIndex = trimmed.indexOf(',');
      int semiColonIndex = trimmed.indexOf(';');
      if (commaIndex > 0 && semiColonIndex > 0 && semiColonIndex < commaIndex) {
        String mimeType = trimmed.substring(5, semiColonIndex);
        String base64 = trimmed.substring(commaIndex + 1);
        return Media.builder().mimeType(MediaType.parseMediaType(mimeType)).data(base64).build();
      }
    }

    if (isBase64(trimmed)) {
      return Media.builder().mimeType(MediaType.IMAGE_PNG).data(trimmed).build();
    }

    return null;
  }

  private boolean isBase64(String str) {
    if (str == null || str.isEmpty()) {
      return false;
    }
    return str.matches("^[A-Za-z0-9+/=]+$") && str.length() % 4 == 0;
  }

  private String chatWithVision(String prompt, List<Media> images) {
    log.info("Processing {} images with Ollama vision model: {}", images.size(), visionModel);
    try {
      ChatClient chatClient =
          chatClientProvider.createStateless(TextChatOptions.ollamaVision(visionModel));

      return chatClient
          .prompt()
          .user(user -> user.text(prompt).media(images.toArray(Media[]::new)))
          .call()
          .content();
    } catch (Exception e) {
      log.error("Error in vision chat: {}", e.getMessage(), e);
      return "Error processing images: " + e.getMessage();
    }
  }

  private String buildPrompt(String question, String context) {
    return localizedRagPromptBuilder.build(question, context);
  }
}
