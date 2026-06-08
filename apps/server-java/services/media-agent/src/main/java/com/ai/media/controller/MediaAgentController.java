package com.ai.media.controller;

import com.ai.media.model.ImageGenerationRequest;
import com.ai.media.model.ImageGenerationResponse;
import com.ai.media.model.LoraInfo;
import com.ai.media.model.ModelInfo;
import com.ai.media.service.ImageProvider;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/image")
public class MediaAgentController {

    private static final Logger log = LoggerFactory.getLogger(MediaAgentController.class);

    private final ImageProvider imageProvider;

    public MediaAgentController(ImageProvider imageProvider) {
        this.imageProvider = imageProvider;
    }

    public record GenerateRequest(
            String prompt,
            String negativePrompt,
            Integer width,
            Integer height,
            Integer steps,
            Float cfgScale,
            Long seed,
            String model,
            String lora
    ) {
        public ImageGenerationRequest toGenerationRequest() {
            return new ImageGenerationRequest(
                    prompt,
                    negativePrompt,
                    width,
                    height,
                    steps,
                    cfgScale,
                    seed,
                    model,
                    lora
            );
        }
    }

    @PostMapping("/generate")
    public Mono<ResponseEntity<ImageGenerationResponse>> generate(
            @Valid @RequestBody GenerateRequest request) {
        log.info("Received image generation request: prompt='{}...'", 
                request.prompt().substring(0, Math.min(50, request.prompt().length())));

        return imageProvider.generate(request.toGenerationRequest())
                .map(ResponseEntity::ok)
                .doOnSuccess(resp -> log.info("Image generated successfully"));
    }

    @PostMapping("/models")
    public Mono<ResponseEntity<List<ModelInfo>>> listModels() {
        log.debug("Listing available models");

        return imageProvider.listModels()
                .map(ResponseEntity::ok);
    }

    @PostMapping("/loras")
    public Mono<ResponseEntity<List<LoraInfo>>> listLoras() {
        log.debug("Listing available LoRAs");

        return imageProvider.listLoras()
                .map(ResponseEntity::ok);
    }

    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> health() {
        return imageProvider.isHealthy()
                .map(healthy -> ResponseEntity.ok(Map.<String, Object>of(
                        "status", healthy ? "healthy" : "unhealthy",
                        "service", "media-agent",
                        "provider", "stable-diffusion"
                )));
    }

    @GetMapping("/models/{modelId}")
    public Mono<ResponseEntity<ModelInfo>> getModel(@PathVariable String modelId) {
        return imageProvider.listModels()
                .map(models -> models.stream()
                        .filter(m -> m.id().equals(modelId))
                        .findFirst()
                        .<ResponseEntity<ModelInfo>>map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build()));
    }
}
