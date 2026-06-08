package com.ai.vision.agent;

import com.ai.common.agent.Agent;
import com.ai.common.agent.AgentRequest;
import com.ai.common.agent.AgentResponse;
import com.ai.common.agent.AgentType;
import com.ai.vision.model.*;
import com.ai.vision.service.VisionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Vision Agent implementing the Agent interface.
 * 
 * This agent handles vision-related requests from the Gateway:
 * - Image analysis (detection, captioning, OCR)
 * - Image generation
 * 
 * The agent delegates actual model inference to VisionService.
 */
@Component
public class VisionAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(VisionAgent.class);

    private final VisionService visionService;

    public VisionAgent(VisionService visionService) {
        this.visionService = visionService;
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
        log.info("Processing Vision request: {}", request.message());

        try {
            // Parse the request to determine what action to take
            Map<String, Object> metadata = request.metadata();
            
            String action = extractAction(request.message(), metadata);
            
            return switch (action.toLowerCase()) {
                case "detect", "detect_objects" -> handleDetection(request, metadata);
                case "caption", "describe" -> handleCaption(request, metadata);
                case "ocr", "read_text" -> handleOcr(request, metadata);
                case "generate", "create_image" -> handleGeneration(request, metadata);
                default -> handleDefaultAnalysis(request, metadata);
            };
        } catch (Exception e) {
            log.error("Vision processing failed: {}", e.getMessage(), e);
            return Mono.just(AgentResponse.error("Vision processing failed: " + e.getMessage()));
        }
    }

    private String extractAction(String message, Map<String, Object> metadata) {
        // Check metadata first
        if (metadata != null && metadata.containsKey("action")) {
            return metadata.get("action").toString();
        }
        
        // Fallback to message content analysis
        String lower = message.toLowerCase();
        if (lower.contains("detect")) return "detect";
        if (lower.contains("caption") || lower.contains("describe")) return "caption";
        if (lower.contains("ocr") || lower.contains("read") || lower.contains("text")) return "ocr";
        if (lower.contains("generate") || lower.contains("create image")) return "generate";
        
        return "analyze";
    }

    private Mono<AgentResponse> handleDetection(AgentRequest request, Map<String, Object> metadata) {
        float confidence = extractConfidence(metadata);
        
        // Note: In real usage, image data would come from metadata or a URL
        // For now, return a placeholder response
        Map<String, Object> resultMetadata = new HashMap<>();
        resultMetadata.put("action", "detect");
        resultMetadata.put("confidence", confidence);
        resultMetadata.put("note", "Image data required for actual detection");
        
        return Mono.just(AgentResponse.success(
            "Vision detection ready. Send image to /api/vision/detect for object detection.",
            type(),
            request.sessionId()
        ).withMetadata(resultMetadata));
    }

    private Mono<AgentResponse> handleCaption(AgentRequest request, Map<String, Object> metadata) {
        return Mono.just(AgentResponse.success(
            "Vision captioning ready. Send image to /api/vision/caption for image description.",
            type(),
            request.sessionId()
        ));
    }

    private Mono<AgentResponse> handleOcr(AgentRequest request, Map<String, Object> metadata) {
        return Mono.just(AgentResponse.success(
            "OCR ready. Send image to /api/vision/ocr for text recognition.",
            type(),
            request.sessionId()
        ));
    }

    private Mono<AgentResponse> handleGeneration(AgentRequest request, Map<String, Object> metadata) {
        String prompt = request.message();
        
        GenerateRequest genRequest = GenerateRequest.defaultConfig(prompt);
        
        return visionService.generateImage(genRequest)
            .map(result -> AgentResponse.success(
                "Generated image: " + result.imageUrl(),
                type(),
                request.sessionId()
            ).withMetadata(Map.of(
                "action", "generate",
                "seed", result.seed(),
                "prompt", prompt
            )))
            .onErrorResume(e -> Mono.just(AgentResponse.error(
                "Image generation failed: " + e.getMessage()
            )));
    }

    private Mono<AgentResponse> handleDefaultAnalysis(AgentRequest request, Map<String, Object> metadata) {
        return Mono.just(AgentResponse.success(
            """
            Vision Agent capabilities:
            - Object Detection: POST /api/vision/detect (with image)
            - Image Captioning: POST /api/vision/caption (with image)
            - OCR Text Recognition: POST /api/vision/ocr (with image)
            - Image Generation: POST /api/vision/generate (with prompt)
            
            Use these endpoints directly for better control over parameters.
            """,
            type(),
            request.sessionId()
        ));
    }

    private float extractConfidence(Map<String, Object> metadata) {
        if (metadata != null && metadata.containsKey("confidence")) {
            Object conf = metadata.get("confidence");
            if (conf instanceof Number) {
                return ((Number) conf).floatValue();
            }
        }
        return 0.5f;
    }
}
