package com.ai.gateway.agent;

import com.ai.common.agent.Agent;
import com.ai.common.agent.AgentRequest;
import com.ai.common.agent.AgentResponse;
import com.ai.common.agent.AgentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Media generation agent delegating to media-agent service (port 8015).
 * Generates images using Stable Diffusion models.
 */
@Component
public class MediaAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(MediaAgent.class);
    private final WebClient webClient;

    public MediaAgent(
            WebClient.Builder webClientBuilder,
            @Value("${ai.agent.services.media.base-url}") String mediaServiceUrl) {
        this.webClient = webClientBuilder.baseUrl(mediaServiceUrl).build();
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
        Map<String, Object> metadata = request.metadata();
        String prompt = request.message();

        Integer width = 512;
        Integer height = 512;
        Integer steps = 20;
        Float cfgScale = 7.0f;
        String negativePrompt = "";
        String model = "";
        String lora = "";

        if (metadata != null) {
            if (metadata.containsKey("width")) width = (Integer) metadata.get("width");
            if (metadata.containsKey("height")) height = (Integer) metadata.get("height");
            if (metadata.containsKey("steps")) steps = (Integer) metadata.get("steps");
            if (metadata.containsKey("cfg_scale")) {
                Object cfgObj = metadata.get("cfg_scale");
                cfgScale = cfgObj instanceof Number n ? n.floatValue() : (Float) cfgObj;
            }
            if (metadata.containsKey("negative_prompt")) negativePrompt = (String) metadata.get("negative_prompt");
            if (metadata.containsKey("model")) model = (String) metadata.get("model");
            if (metadata.containsKey("lora")) lora = (String) metadata.get("lora");
        }

        Map<String, Object> body = Map.of(
                "prompt", prompt,
                "width", width,
                "height", height,
                "steps", steps,
                "cfgScale", cfgScale,
                "negativePrompt", negativePrompt,
                "model", model,
                "lora", lora
        );

        log.info("Generating image with prompt: {}...", prompt.substring(0, Math.min(50, prompt.length())));

        return webClient.post()
                .uri("/image/generate")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    String imageUrl = String.valueOf(response.getOrDefault("image_url", ""));
                    String status = String.valueOf(response.getOrDefault("status", "unknown"));
                    Long seed = null;
                    if (response.containsKey("seed") && response.get("seed") instanceof Number num) {
                        seed = num.longValue();
                    }

                    Map<String, Object> resultMetadata = new java.util.HashMap<>();
                    resultMetadata.put("status", status);
                    if (seed != null) resultMetadata.put("seed", seed);
                    if (!imageUrl.isEmpty()) resultMetadata.put("image_url", imageUrl);

                    return AgentResponse.success(
                            "Image generated successfully. URL: " + imageUrl,
                            type()
                    ).withMetadata(resultMetadata);
                })
                .doOnSuccess(resp -> log.info("Image generation completed"))
                .doOnError(e -> log.error("Image generation failed: {}", e.getMessage()))
                .onErrorResume(e -> {
                    log.error("Media service error: {}", e.getMessage());
                    return Mono.just(AgentResponse.error("Image generation failed: " + e.getMessage()));
                });
    }
}
