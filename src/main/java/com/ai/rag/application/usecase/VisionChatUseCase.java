package com.ai.rag.application.usecase;

import com.ai.common.application.llm.ChatClientProvider;
import com.ai.common.application.llm.TextChatOptions;
import com.ai.common.util.LogSanitizer;
import com.ai.rag.application.dto.RagChatResult;
import com.ai.rag.domain.vo.DocumentId;
import com.ai.chat.domain.service.LanguageDetectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Service
@ConditionalOnProperty(name = "spring.ai.ollama.chat.enabled", havingValue = "true", matchIfMissing = true)
public class VisionChatUseCase {

    private static final Logger log = LoggerFactory.getLogger(VisionChatUseCase.class);
    private static final int DEFAULT_TOP_K = 5;

    @Value("${spring.ai.ollama.chat.model:qwen3.5:35b}")
    private String visionModel;

    private final RagApplicationService ragApplicationService;
    private final ChatClientProvider chatClientProvider;
    private final LanguageDetectionService languageDetectionService;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public VisionChatUseCase(
            RagApplicationService ragApplicationService,
            ChatClientProvider chatClientProvider,
            LanguageDetectionService languageDetectionService) {
        this.ragApplicationService = ragApplicationService;
        this.chatClientProvider = chatClientProvider;
        this.languageDetectionService = languageDetectionService;
    }

    public RagChatResult chatWithImages(String question, List<String> docIds, List<String> images, Integer topK) {
        log.info("Vision RAG chat request: {} with {} images",
                LogSanitizer.truncate(question),
                images != null ? images.size() : 0);

        List<Media> mediaList = parseImages(images);

        List<DocumentId> docIdList = null;
        if (docIds != null && !docIds.isEmpty()) {
            docIdList = docIds.stream()
                    .map(DocumentId::of)
                    .toList();
        }

        int topKValue = topK != null ? topK : DEFAULT_TOP_K;
        var retrievalResult = ragApplicationService.retrieveContext(question, docIdList, topKValue);

        String context = retrievalResult.context();
        var sources = retrievalResult.sources();

        String prompt = buildPrompt(question, context);
        String aiResponse = chatWithVision(prompt, mediaList);

        log.info("Vision RAG chat completed successfully");
        return new RagChatResult(aiResponse, sources);
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
                byte[] imageBytes = httpClient
                        .send(HttpRequest.newBuilder()
                                        .uri(URI.create(trimmed))
                                        .GET()
                                        .build(),
                                HttpResponse.BodyHandlers.ofByteArray())
                        .body();

                String base64 = java.util.Base64.getEncoder().encodeToString(imageBytes);
                return Media.builder()
                        .mimeType(MediaType.IMAGE_PNG)
                        .data(base64)
                        .build();
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
                return Media.builder()
                        .mimeType(MediaType.parseMediaType(mimeType))
                        .data(base64)
                        .build();
            }
        }

        if (isBase64(trimmed)) {
            return Media.builder()
                    .mimeType(MediaType.IMAGE_PNG)
                    .data(trimmed)
                    .build();
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
            ChatClient chatClient = chatClientProvider.createStateless(
                    TextChatOptions.ollamaVision(visionModel));

            return chatClient.prompt()
                    .user(user -> user.text(prompt).media(images.toArray(Media[]::new)))
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("Error in vision chat: {}", e.getMessage(), e);
            return "Error processing images: " + e.getMessage();
        }
    }

    private String buildPrompt(String question, String context) {
        String languageCode = languageDetectionService.detect(question);
        return languageDetectionService.buildPrompt(question, context, languageCode);
    }
}
