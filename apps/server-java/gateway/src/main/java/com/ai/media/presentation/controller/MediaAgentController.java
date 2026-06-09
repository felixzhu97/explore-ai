package com.ai.media.presentation.controller;

import com.ai.media.application.service.MediaApplicationService;
import com.ai.media.domain.GenerationParams;
import com.ai.media.domain.ModelInfo;
import com.ai.media.domain.LoraInfo;
import com.ai.media.presentation.dto.ImageGenerationRequest;
import com.ai.media.presentation.dto.ImageGenerationResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * REST controller for media/image generation operations.
 * Thin controller layer that delegates to Application Service.
 */
@RestController
@RequestMapping("/api/image")
public class MediaAgentController {

    private static final Logger log = LoggerFactory.getLogger(MediaAgentController.class);

    private final MediaApplicationService mediaService;

    public MediaAgentController(MediaApplicationService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping("/generate")
    public Mono<ResponseEntity<ImageGenerationResponse>> generate(
            @Valid @RequestBody ImageGenerationRequest request) {
        log.info("Received image generation request: prompt='{}...'", 
                truncatePrompt(request.prompt()));

        GenerationParams params = GenerationParams.builder()
                .prompt(request.prompt())
                .negativePrompt(request.negativePrompt())
                .width(request.width())
                .height(request.height())
                .steps(request.steps())
                .cfgScale(request.cfgScale())
                .seed(request.seed())
                .model(request.model())
                .lora(request.lora())
                .build();

        return mediaService.generate(params)
                .map(result -> ResponseEntity.ok(new ImageGenerationResponse(
                        result.images(),
                        result.seed(),
                        result.model(),
                        result.prompt(),
                        result.width(),
                        result.height(),
                        result.numInferenceSteps(),
                        result.guidanceScale(),
                        result.processingTimeMs(),
                        result.success(),
                        result.error()
                )))
                .doOnSuccess(resp -> {
                    if (resp.getBody() != null && resp.getBody().success()) {
                        log.info("Image generated successfully");
                    }
                })
                .doOnError(error -> log.error("Image generation failed: {}", error.getMessage(), error));
    }

    @PostMapping("/models")
    public Mono<ResponseEntity<List<ModelInfo>>> listModels() {
        log.debug("Listing available models");

        return mediaService.listModels()
                .map(ResponseEntity::ok);
    }

    @GetMapping("/models/{modelId}")
    public Mono<ResponseEntity<ModelInfo>> getModel(@PathVariable String modelId) {
        return mediaService.getModel(modelId)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.notFound().build()));
    }

    @PostMapping("/loras")
    public Mono<ResponseEntity<List<LoraInfo>>> listLoras() {
        log.debug("Listing available LoRAs");

        return mediaService.listLoras()
                .map(ResponseEntity::ok);
    }

    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> health() {
        return mediaService.isHealthy()
                .map(healthy -> ResponseEntity.ok(Map.<String, Object>of(
                        "status", healthy ? "healthy" : "unhealthy",
                        "service", "media-agent",
                        "provider", "stable-diffusion"
                )));
    }

    private String truncatePrompt(String prompt) {
        if (prompt == null) return "";
        return prompt.substring(0, Math.min(50, prompt.length()));
    }
}
