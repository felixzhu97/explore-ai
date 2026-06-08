package com.ai.gateway.agent;

import com.ai.common.agent.Agent;
import com.ai.common.agent.AgentRequest;
import com.ai.common.agent.AgentResponse;
import com.ai.common.agent.AgentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Vision agent delegating to external Vision service (port 8000).
 * Supports object detection, image captioning, OCR, and multi-task analysis.
 */
@Component
public class VisionAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(VisionAgent.class);
    private final WebClient webClient;

    public VisionAgent(
            WebClient.Builder webClientBuilder,
            @Value("${ai.agent.services.vision.base-url}") String visionServiceUrl) {
        this.webClient = webClientBuilder.baseUrl(visionServiceUrl).build();
    }

    @Override
    public String name() {
        return "VisionAgent";
    }

    @Override
    public AgentType type() {
        return AgentType.VISION;
    }

    @Override
    public Mono<AgentResponse> process(AgentRequest request) {
        Map<String, Object> metadata = request.metadata();
        String operation = metadata != null ? (String) metadata.getOrDefault("operation", "analyze") : "analyze";

        return switch (operation) {
            case "detect" -> handleDetect(request);
            case "caption" -> handleCaption(request);
            case "ocr" -> handleOcr(request);
            default -> handleAnalyze(request);
        };
    }

    private Mono<AgentResponse> handleDetect(AgentRequest request) {
        return sendImageRequest("/api/vision/detect", request)
                .map(response -> {
                    String result = formatDetectionResult(response);
                    return AgentResponse.success(result, type());
                });
    }

    private Mono<AgentResponse> handleCaption(AgentRequest request) {
        return sendImageRequest("/api/vision/caption", request)
                .map(response -> {
                    String caption = extractCaption(response);
                    return AgentResponse.success(caption, type());
                });
    }

    private Mono<AgentResponse> handleOcr(AgentRequest request) {
        return sendImageRequest("/api/vision/ocr", request)
                .map(response -> {
                    String text = extractOcrText(response);
                    return AgentResponse.success(text, type());
                });
    }

    private Mono<AgentResponse> handleAnalyze(AgentRequest request) {
        Map<String, Object> metadata = request.metadata();
        String task = metadata != null ? (String) metadata.getOrDefault("task", "caption_image") : "caption_image";

        return sendImageRequest("/api/vision/analyze?task=" + task, request)
                .map(response -> {
                    String result = formatAnalyzeResult(response);
                    return AgentResponse.success(result, type())
                            .withMetadata(Map.of("task", task));
                });
    }

    private Mono<Map> sendImageRequest(String endpoint, AgentRequest request) {
        byte[] imageData = extractImageData(request);

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", new ByteArrayResource(imageData) {
            @Override
            public String getFilename() {
                return "image.jpg";
            }
        }, MediaType.IMAGE_JPEG);

        return webClient.post()
                .uri(endpoint)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(bodyBuilder.build())
                .retrieve()
                .bodyToMono(Map.class)
                .doOnNext(r -> log.debug("Vision response received: {}", r.keySet()))
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("Vision service error: {} {}", e.getStatusCode(), e.getMessage());
                    return Mono.error(new RuntimeException("Vision service error: " + e.getMessage()));
                })
                .onErrorResume(e -> {
                    if (e instanceof RuntimeException) throw (RuntimeException) e;
                    return Mono.error(new RuntimeException("Vision request failed: " + e.getMessage()));
                });
    }

    private byte[] extractImageData(AgentRequest request) {
        Map<String, Object> metadata = request.metadata();
        if (metadata != null && metadata.containsKey("image_data")) {
            Object data = metadata.get("image_data");
            if (data instanceof String base64) {
                return java.util.Base64.getDecoder().decode(base64);
            }
        }
        return new byte[0];
    }

    private String formatDetectionResult(Map response) {
        if (response.containsKey("detections")) {
            return "Objects detected: " + response.get("detections");
        }
        return response.toString();
    }

    private String extractCaption(Map response) {
        if (response.containsKey("caption")) {
            return String.valueOf(response.get("caption"));
        }
        if (response.containsKey("description")) {
            return String.valueOf(response.get("description"));
        }
        return response.toString();
    }

    private String extractOcrText(Map response) {
        if (response.containsKey("text")) {
            return String.valueOf(response.get("text"));
        }
        if (response.containsKey("extracted_text")) {
            return String.valueOf(response.get("extracted_text"));
        }
        return response.toString();
    }

    private String formatAnalyzeResult(Map response) {
        return response.toString();
    }
}
