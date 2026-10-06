package com.ai.vision.service;

import com.ai.chat.infra.prompt.LocalizedRagPromptBuilder;
import com.ai.common.infra.logging.LogSanitizer;
import com.ai.common.service.llm.ChatClientProvider;
import com.ai.common.service.llm.StreamTokenEvent;
import com.ai.common.service.llm.TextChatOptions;
import com.ai.rag.domain.model.SourceDocument;
import com.ai.rag.domain.vo.DocumentId;
import com.ai.rag.service.RagApplicationService;
import com.ai.rag.service.dto.RagSourceEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
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

/** Multimodal RAG chat that sends images plus retrieved context to the Ollama vision model. */
@Service
@ConditionalOnProperty(
    name = "spring.ai.ollama.chat.enabled",
    havingValue = "true",
    matchIfMissing = true)
@RequiredArgsConstructor
public class VisionChatService {

  private static final Logger log = LoggerFactory.getLogger(VisionChatService.class);

  @Value("${spring.ai.ollama.chat.model:qwen3.5:35b}")
  private String visionModel;

  private final RagApplicationService ragApplicationService;
  private final ChatClientProvider chatClientProvider;
  private final LocalizedRagPromptBuilder localizedRagPromptBuilder;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  /** True token streaming via ChatClient; emits {@code sources} SSE after content completes. */
  public Flux<ServerSentEvent<String>> streamChatWithImages(
      String question, List<String> documentIds, List<String> images, int topK, String ownerKey) {
    log.info(
        "Vision RAG stream request: {} with {} images",
        LogSanitizer.truncate(question),
        images.size());

    List<Media> mediaList = parseImages(images);
    List<DocumentId> documentIdList = toDocumentIds(documentIds);
    var retrievalResult =
        ragApplicationService.retrieveContext(question, documentIdList, topK, ownerKey);
    String prompt = buildPrompt(question, retrievalResult.context());
    List<SourceDocument> sources = retrievalResult.sources();

    return streamVision(prompt, mediaList)
        .concatWith(Flux.defer(() -> buildSourceEvents(sources)))
        .doOnComplete(() -> log.info("Vision RAG stream completed successfully"));
  }

  private List<DocumentId> toDocumentIds(List<String> documentIds) {
    if (documentIds == null || documentIds.isEmpty()) {
      return null;
    }
    return documentIds.stream().map(DocumentId::of).toList();
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
          .filter(piece -> !piece.isEmpty())
          .map(
              piece ->
                  ServerSentEvent.<String>builder().data(StreamTokenEvent.toJson(piece)).build())
          .onErrorResume(
              ex -> {
                log.error("Error in vision stream: {}", ex.getMessage(), ex);
                return Flux.just(buildErrorEvent("Error processing images: " + ex.getMessage()));
              });
    } catch (RuntimeException ex) {
      log.error("Error starting vision stream: {}", ex.getMessage(), ex);
      return Flux.just(buildErrorEvent("Error processing images: " + ex.getMessage()));
    }
  }

  private static ServerSentEvent<String> buildErrorEvent(String message) {
    return ServerSentEvent.<String>builder().event("error").data(message).build();
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
      log.warn("Failed to serialize vision RAG sources for SSE", ex);
      return Flux.empty();
    }
  }

  private List<Media> parseImages(List<String> images) {
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
    return !str.isEmpty() && str.matches("^[A-Za-z0-9+/=]+$") && str.length() % 4 == 0;
  }

  private String buildPrompt(String question, String context) {
    return localizedRagPromptBuilder.build(question, context);
  }
}
