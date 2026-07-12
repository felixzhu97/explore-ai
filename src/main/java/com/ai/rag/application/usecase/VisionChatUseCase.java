package com.ai.rag.application.usecase;

import com.ai.rag.domain.model.SourceDocument;
import com.ai.rag.domain.vo.DocumentId;
import com.ai.chat.domain.service.LanguageDetectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.content.Media;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;

/**
 * Vision-enabled RAG chat use case.
 * Uses Ollama qwen3.5:35b for multimodal understanding (open-source).
 */
@Service
@ConditionalOnProperty(name = "spring.ai.ollama.chat.enabled", havingValue = "true", matchIfMissing = true)
public class VisionChatUseCase {

    private static final Logger log = LoggerFactory.getLogger(VisionChatUseCase.class);
    private static final int DEFAULT_TOP_K = 5;

    @Value("${spring.ai.ollama.chat.model:qwen3.5:35b}")
    private String visionModel;

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    private final RagApplicationService ragApplicationService;
    private final ChatModel chatModel;
    private final LanguageDetectionService languageDetectionService;

    public VisionChatUseCase(
            RagApplicationService ragApplicationService,
            LanguageDetectionService languageDetectionService,
            @Qualifier("ollamaVisionChatModel") ChatModel chatModel) {
        this.ragApplicationService = ragApplicationService;
        this.languageDetectionService = languageDetectionService;
        this.chatModel = chatModel;
    }

    /**
     * Result of vision-enabled RAG chat operation.
     */
    public record ChatResult(
            String response,
            List<SourceDocument> sources
    ) {}

    /**
     * Executes vision-enabled RAG chat: retrieves context, builds prompt with images, and gets AI response.
     */
    public ChatResult chatWithImages(String question, List<String> docIds, List<String> images, Integer topK) {
        log.info("Vision RAG chat request: {} with {} images",
                question != null && question.length() > 50 ? question.substring(0, 50) + "..." : question,
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
        List<SourceDocument> sources = retrievalResult.sources();

        String prompt = buildPrompt(question, context);
        String aiResponse = chatWithVision(prompt, mediaList);

        log.info("Vision RAG chat completed successfully");
        return new ChatResult(aiResponse, sources);
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

        // Ollama vision supports URL and base64
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            try {
                byte[] imageBytes = java.net.http.HttpClient.newHttpClient()
                        .send(java.net.http.HttpRequest.newBuilder()
                                        .uri(URI.create(trimmed))
                                        .GET()
                                        .build(),
                                java.net.http.HttpResponse.BodyHandlers.ofByteArray())
                        .body();

                String base64 = java.util.Base64.getEncoder().encodeToString(imageBytes);
                return Media.builder()
                        .mimeType(MediaType.IMAGE_PNG)
                        .data(base64)  // Ollama expects raw base64 without prefix
                        .build();
            } catch (Exception e) {
                log.warn("Failed to fetch image from URL {}: {}", trimmed, e.getMessage());
                return null;
            }
        }

        // Data URL format: data:image/png;base64,xxxxx
        if (trimmed.startsWith("data:image/")) {
            int commaIndex = trimmed.indexOf(',');
            if (commaIndex > 0) {
                String base64 = trimmed.substring(commaIndex + 1);
                return Media.builder()
                        .mimeType(MediaType.IMAGE_PNG)
                        .data(base64)  // Ollama expects raw base64
                        .build();
            }
        }

        // Raw base64
        if (isBase64(trimmed)) {
            return Media.builder()
                    .mimeType(MediaType.IMAGE_PNG)
                    .data(trimmed)
                    .build();
        }

        return null;
    }

    private boolean isBase64(String str) {
        if (str == null || str.isEmpty()) return false;
        // Check if it's valid base64
        return str.matches("^[A-Za-z0-9+/=]+$") && str.length() % 4 == 0;
    }

    private String chatWithVision(String prompt, List<Media> images) {
        log.info("Processing {} images with Ollama vision model: {}", images.size(), visionModel);
        try {
            UserMessage userMessage = UserMessage.builder()
                    .text(prompt)
                    .media(images)
                    .build();

            // Use OllamaChatModel directly with qwen3.5:35b
            var response = chatModel.call(
                    new org.springframework.ai.chat.prompt.Prompt(
                            List.of(userMessage),
                            OllamaChatOptions.builder()
                                    .model(visionModel)
                                    .build()
                    )
            );

            return response.getResult().getOutput().getText();
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
