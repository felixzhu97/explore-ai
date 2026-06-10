package com.ai.agents.infrastructure.adapter;

import com.ai.agents.domain.AgentType;
import com.ai.agents.domain.Conversation;
import com.ai.agents.presentation.dto.AgentRequestDto;
import com.ai.agents.presentation.dto.AgentResponseDto;
import com.ai.media.application.service.MediaApplicationService;
import com.ai.media.domain.GenerationParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Media Agent Adapter.
 * Delegates to internal MediaApplicationService instead of external WebClient call.
 */
@Component
public class MediaAgentAdapter implements AgentAdapter {

    private static final Logger log = LoggerFactory.getLogger(MediaAgentAdapter.class);

    private final MediaApplicationService mediaService;

    public MediaAgentAdapter(MediaApplicationService mediaService) {
        this.mediaService = mediaService;
    }

    @Override
    public AgentType getType() {
        return AgentType.MEDIA;
    }

    @Override
    public Mono<AgentResponseDto> execute(Conversation conversation, AgentRequestDto request) {
        log.info("Media agent processing request: {}", truncate(request.getUserMessage(), 50));

        Map<String, Object> metadata = request.metadata();
        String prompt = request.getUserMessage();

        Integer width = metadata != null && metadata.containsKey("width") 
            ? ((Number) metadata.get("width")).intValue() : 512;
        Integer height = metadata != null && metadata.containsKey("height") 
            ? ((Number) metadata.get("height")).intValue() : 512;
        Integer steps = metadata != null && metadata.containsKey("steps") 
            ? ((Number) metadata.get("steps")).intValue() : 25;
        Float cfgScale = metadata != null && metadata.containsKey("cfg_scale") 
            ? ((Number) metadata.get("cfg_scale")).floatValue() : 7.5f;
        String negativePrompt = metadata != null ? (String) metadata.get("negative_prompt") : "";
        String model = metadata != null ? (String) metadata.get("model") : null;

        GenerationParams params = GenerationParams.builder()
                .prompt(prompt)
                .negativePrompt(negativePrompt)
                .width(width)
                .height(height)
                .steps(steps)
                .cfgScale(cfgScale)
                .model(model)
                .build();

        return mediaService.generate(params)
                .map(result -> {
                    if (result.success()) {
                        String imageUrl = result.images() != null && !result.images().isEmpty()
                            ? "data:image/png;base64," + result.images().get(0)
                            : "";
                        Map<String, Object> responseMetadata = Map.of(
                                "seed", result.seed(),
                                "model", result.model(),
                                "width", result.width(),
                                "height", result.height(),
                                "processingTimeMs", result.processingTimeMs(),
                                "imageUrl", imageUrl
                        );
                        return AgentResponseDto.success(
                                        "Image generated successfully. Seed: " + result.seed(),
                                        AgentType.MEDIA
                                )
                                .withMetadata(responseMetadata);
                    } else {
                        return AgentResponseDto.error("Image generation failed: " + result.error());
                    }
                })
                .doOnSuccess(r -> log.info("Media agent completed: {}", r.message()))
                .doOnError(e -> log.error("Media agent failed", e))
                .onErrorResume(e -> Mono.just(AgentResponseDto.error("Media generation failed: " + e.getMessage())));
    }

    @Override
    public boolean isAvailable() {
        return mediaService != null;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
