package com.ai.media.agent;

import com.ai.common.agent.Agent;
import com.ai.common.agent.AgentRequest;
import com.ai.common.agent.AgentResponse;
import com.ai.common.agent.AgentType;
import com.ai.media.service.ImageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Media generation agent that handles image generation requests.
 * Integrates with the Gateway framework via the Agent interface.
 */
@Component
public class MediaAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(MediaAgent.class);

    private final ImageProvider imageProvider;

    public MediaAgent(ImageProvider imageProvider) {
        this.imageProvider = imageProvider;
    }

    @Override
    public String name() {
        return "MediaAgent";
    }

    @Override
    public AgentType type() {
        return AgentType.MEDIA;
    }

    @Override
    public Mono<AgentResponse> process(AgentRequest request) {
        log.info("Processing media request: {}", request.message());

        return imageProvider.isHealthy()
                .flatMap(healthy -> {
                    if (!healthy) {
                        return Mono.just(AgentResponse.error(
                                "Image generation service is not available"));
                    }

                    var metadata = request.metadata();
                    String prompt = request.message();
                    String model = request.model();
                    Long seed = extractSeed(metadata);
                    Integer width = extractInt(metadata, "width", 512);
                    Integer height = extractInt(metadata, "height", 512);
                    Integer steps = extractInt(metadata, "steps", 25);
                    Float cfgScale = extractFloat(metadata, "cfgScale", 7.5f);

                    var genRequest = new com.ai.media.model.ImageGenerationRequest(
                            prompt,
                            extractString(metadata, "negativePrompt", 
                                    "blurry, ugly, distorted, low quality"),
                            width,
                            height,
                            steps,
                            cfgScale,
                            seed,
                            model,
                            extractString(metadata, "lora", null)
                    );

                    return imageProvider.generate(genRequest)
                            .map(response -> {
                                Map<String, Object> responseMetadata = Map.of(
                                        "images", response.images(),
                                        "seed", response.seed(),
                                        "processingTimeMs", response.processingTimeMs()
                                );
                                return AgentResponse.success(
                                        String.format("Generated %d image(s) in %.0fms",
                                                response.images().size(),
                                                response.processingTimeMs()),
                                        type()
                                ).withMetadata(responseMetadata);
                            });
                })
                .onErrorResume(e -> {
                    log.error("Media processing failed: {}", e.getMessage(), e);
                    return Mono.just(AgentResponse.error(
                            "Image generation failed: " + e.getMessage()));
                });
    }

    private Long extractSeed(Map<String, Object> metadata) {
        if (metadata == null) return null;
        Object seed = metadata.get("seed");
        if (seed instanceof Number) {
            return ((Number) seed).longValue();
        }
        return null;
    }

    private Integer extractInt(Map<String, Object> metadata, String key, int defaultValue) {
        if (metadata == null) return defaultValue;
        Object value = metadata.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    private Float extractFloat(Map<String, Object> metadata, String key, float defaultValue) {
        if (metadata == null) return defaultValue;
        Object value = metadata.get(key);
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        return defaultValue;
    }

    private String extractString(Map<String, Object> metadata, String key, String defaultValue) {
        if (metadata == null) return defaultValue;
        Object value = metadata.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}
