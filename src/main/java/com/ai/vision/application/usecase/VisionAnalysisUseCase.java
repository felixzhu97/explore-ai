package com.ai.vision.application.usecase;

import com.ai.vision.web.dto.CaptionResponse;
import com.ai.vision.web.dto.DetectResponse;
import com.ai.vision.web.dto.DetectionDto;
import com.ai.vision.web.dto.OcrResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class VisionAnalysisUseCase {

    private static final Logger log = LoggerFactory.getLogger(VisionAnalysisUseCase.class);
    private static final Pattern JSON_ARRAY_PATTERN = Pattern.compile("\\[.*]", Pattern.DOTALL);

    private final ChatModel visionChatModel;
    private final ObjectMapper objectMapper;

    @Value("${spring.ai.ollama.chat.model:qwen3.5:35b}")
    private String visionModel;

    public VisionAnalysisUseCase(
            @Qualifier("ollamaVisionChatModel") ChatModel visionChatModel,
            ObjectMapper objectMapper) {
        this.visionChatModel = visionChatModel;
        this.objectMapper = objectMapper;
    }

    public CaptionResponse caption(MultipartFile file) throws IOException {
        long startedAt = System.nanoTime();
        String caption = analyzeImage(
                file,
                "Describe this image in one concise sentence.");
        return new CaptionResponse(caption, elapsedMillis(startedAt));
    }

    public OcrResponse ocr(MultipartFile file) throws IOException {
        long startedAt = System.nanoTime();
        String fullText = analyzeImage(
                file,
                "Extract all visible text from this image. Return plain text only.");
        return new OcrResponse(fullText, elapsedMillis(startedAt));
    }

    public DetectResponse detect(MultipartFile file) throws IOException {
        long startedAt = System.nanoTime();
        String response = analyzeImage(
                file,
                """
                Detect objects in this image.
                Return ONLY a JSON array with objects shaped like:
                [{"class_name":"label","confidence":0.95,"bbox":[x,y,width,height]}]
                """);
        List<DetectionDto> detections = parseDetections(response);
        return new DetectResponse(detections, elapsedMillis(startedAt));
    }

    private String analyzeImage(MultipartFile file, String prompt) throws IOException {
        Media media = toMedia(file);
        UserMessage userMessage = UserMessage.builder()
                .text(prompt)
                .media(List.of(media))
                .build();

        var response = visionChatModel.call(
                new Prompt(
                        List.of(userMessage),
                        OllamaChatOptions.builder().model(visionModel).build()));

        return response.getResult().getOutput().getText();
    }

    private Media toMedia(MultipartFile file) throws IOException {
        String mimeType = StringUtils.hasText(file.getContentType())
                ? file.getContentType()
                : MediaType.IMAGE_JPEG_VALUE;
        String base64 = Base64.getEncoder().encodeToString(file.getBytes());
        return Media.builder()
                .mimeType(MediaType.parseMediaType(mimeType))
                .data(base64)
                .build();
    }

    private List<DetectionDto> parseDetections(String response) {
        if (!StringUtils.hasText(response)) {
            return List.of();
        }

        try {
            return objectMapper.readValue(extractJsonArray(response), new TypeReference<>() {});
        } catch (Exception ex) {
            log.warn("Failed to parse detection JSON: {}", ex.getMessage());
            return List.of();
        }
    }

    private String extractJsonArray(String response) {
        Matcher matcher = JSON_ARRAY_PATTERN.matcher(response.trim());
        if (matcher.find()) {
            return matcher.group();
        }
        return response.trim();
    }

    private long elapsedMillis(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000L;
    }
}
