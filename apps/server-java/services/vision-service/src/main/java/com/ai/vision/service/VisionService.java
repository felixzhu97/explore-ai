package com.ai.vision.service;

import com.ai.vision.model.*;
import com.ai.vision.provider.*;
import com.ai.vision.provider.VisionModel.ModelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Unified Vision Service that orchestrates all vision model providers.
 * 
 * This service:
 * 1. Aggregates all VisionModel providers (YOLO, BLIP, OCR, Stable Diffusion)
 * 2. Provides a unified API for the controller
 * 3. Handles provider selection and fallback
 */
@Service
public class VisionService {

    private static final Logger log = LoggerFactory.getLogger(VisionService.class);

    private final Map<ModelType, VisionModel> providers;

    public VisionService(List<VisionModel> visionModels) {
        this.providers = visionModels.stream()
            .collect(Collectors.toMap(
                VisionModel::type,
                Function.identity()
            ));
        
        log.info("VisionService initialized with {} providers: {}", 
                 providers.size(), providers.keySet());
    }

    /**
     * Object detection using YOLO.
     */
    public Mono<DetectionResponse> detectObjects(byte[] imageData, float confidence) {
        VisionModel provider = providers.get(ModelType.YOLO);
        
        if (provider == null) {
            return Mono.error(new IllegalStateException("YOLO provider not available"));
        }

        return provider.detect(imageData, confidence)
            .doOnSuccess(result -> log.info("Detection completed: {} objects found", 
                    result.objects().size()))
            .doOnError(e -> log.error("Detection failed: {}", e.getMessage()));
    }

    /**
     * Image captioning using BLIP.
     */
    public Mono<CaptionResponse> captionImage(byte[] imageData) {
        VisionModel provider = providers.get(ModelType.BLIP);
        
        if (provider == null) {
            return Mono.error(new IllegalStateException("BLIP provider not available"));
        }

        return provider.caption(imageData)
            .doOnSuccess(result -> log.info("Captioning completed: {}", result.caption()))
            .doOnError(e -> log.error("Captioning failed: {}", e.getMessage()));
    }

    /**
     * OCR text recognition.
     */
    public Mono<OcrResponse> recognizeText(byte[] imageData, String language) {
        VisionModel provider = providers.get(ModelType.OCR);
        
        if (provider == null) {
            return Mono.error(new IllegalStateException("OCR provider not available"));
        }

        return provider.recognizeText(imageData, language)
            .doOnSuccess(result -> log.info("OCR completed: {} characters recognized", 
                    result.text().length()))
            .doOnError(e -> log.error("OCR failed: {}", e.getMessage()));
    }

    /**
     * Image generation using Stable Diffusion.
     */
    public Mono<GenerateResponse> generateImage(GenerateRequest request) {
        VisionModel provider = providers.get(ModelType.STABLE_DIFFUSION);
        
        if (provider == null) {
            return Mono.error(new IllegalStateException("Stable Diffusion provider not available"));
        }

        return provider.generate(request)
            .doOnSuccess(result -> log.info("Image generation completed: {}", result.imageUrl()))
            .doOnError(e -> log.error("Generation failed: {}", e.getMessage()));
    }

    /**
     * Check if a specific provider is available.
     */
    public boolean isProviderAvailable(ModelType type) {
        VisionModel provider = providers.get(type);
        return provider != null && provider.isAvailable();
    }

    /**
     * Get all available providers.
     */
    public Map<ModelType, Boolean> getProviderStatus() {
        return providers.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().isAvailable()
            ));
    }
}
