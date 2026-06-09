package com.ai.agents.infrastructure.adapter;

import com.ai.agents.domain.AgentType;
import com.ai.agents.domain.Conversation;
import com.ai.agents.presentation.dto.AgentRequestDto;
import com.ai.agents.presentation.dto.AgentResponseDto;
import com.ai.vision.application.service.VisionApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Vision Agent Adapter.
 * Delegates to internal VisionApplicationService instead of external WebClient call.
 */
@Component
public class VisionAgentAdapter implements AgentAdapter {

    private static final Logger log = LoggerFactory.getLogger(VisionAgentAdapter.class);

    private final VisionApplicationService visionService;

    public VisionAgentAdapter(VisionApplicationService visionService) {
        this.visionService = visionService;
    }

    @Override
    public AgentType getType() {
        return AgentType.VISION;
    }

    @Override
    public Mono<AgentResponseDto> execute(Conversation conversation, AgentRequestDto request) {
        log.info("Vision agent processing request");

        Map<String, Object> metadata = request.metadata();
        String operation = metadata != null ? (String) metadata.getOrDefault("operation", "analyze") : "analyze";

        return switch (operation) {
            case "detect" -> handleDetect(request);
            case "caption" -> handleCaption(request);
            case "ocr" -> handleOcr(request);
            default -> handleAnalyze(request, metadata);
        };
    }

    private Mono<AgentResponseDto> handleDetect(AgentRequestDto request) {
        byte[] imageData = extractImageData(request);

        return visionService.detectObjects(imageData, 0.5f)
                .map(result -> {
                    String detections = result.objects().stream()
                            .map(obj -> obj.label() + " (" + String.format("%.2f", obj.confidence()) + ")")
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("No objects detected");
                    return AgentResponseDto.success("Objects detected: " + detections, AgentType.VISION);
                })
                .onErrorResume(e -> {
                    log.error("Vision detect failed", e);
                    return Mono.just(AgentResponseDto.error("Detection failed: " + e.getMessage()));
                });
    }

    private Mono<AgentResponseDto> handleCaption(AgentRequestDto request) {
        byte[] imageData = extractImageData(request);

        return visionService.captionImage(imageData)
                .map(result -> AgentResponseDto.success(result.caption(), AgentType.VISION))
                .onErrorResume(e -> {
                    log.error("Vision caption failed", e);
                    return Mono.just(AgentResponseDto.error("Caption failed: " + e.getMessage()));
                });
    }

    private Mono<AgentResponseDto> handleOcr(AgentRequestDto request) {
        byte[] imageData = extractImageData(request);
        Map<String, Object> metadata = request.metadata();
        String language = metadata != null ? (String) metadata.get("language") : "eng";

        return visionService.recognizeText(imageData, language)
                .map(result -> AgentResponseDto.success(result.fullText(), AgentType.VISION))
                .onErrorResume(e -> {
                    log.error("Vision OCR failed", e);
                    return Mono.just(AgentResponseDto.error("OCR failed: " + e.getMessage()));
                });
    }

    private Mono<AgentResponseDto> handleAnalyze(AgentRequestDto request, Map<String, Object> metadata) {
        byte[] imageData = extractImageData(request);

        return visionService.captionImage(imageData)
                .map(result -> AgentResponseDto.success(result.caption(), AgentType.VISION))
                .onErrorResume(e -> {
                    log.error("Vision analyze failed", e);
                    return Mono.just(AgentResponseDto.error("Analysis failed: " + e.getMessage()));
                });
    }

    private byte[] extractImageData(AgentRequestDto request) {
        Map<String, Object> metadata = request.metadata();
        if (metadata != null && metadata.containsKey("image_data")) {
            Object data = metadata.get("image_data");
            if (data instanceof String base64) {
                return java.util.Base64.getDecoder().decode(base64);
            }
        }
        return new byte[0];
    }

    @Override
    public boolean isAvailable() {
        return visionService != null;
    }
}
