package com.ai.media.service;

import com.ai.media.model.ImageGenerationRequest;
import com.ai.media.model.ImageGenerationResponse;
import com.ai.media.model.LoraInfo;
import com.ai.media.model.ModelInfo;
import reactor.core.publisher.Mono;

/**
 * Port interface for image generation providers.
 * Implements the Adapter pattern to support multiple image generation backends.
 */
public interface ImageProvider {

    /**
     * Generate images from text prompt.
     *
     * @param request generation parameters
     * @return generated images with metadata
     */
    Mono<ImageGenerationResponse> generate(ImageGenerationRequest request);

    /**
     * List available models.
     *
     * @return list of available models
     */
    Mono<java.util.List<ModelInfo>> listModels();

    /**
     * List available LoRA models.
     *
     * @return list of available LoRAs
     */
    Mono<java.util.List<LoraInfo>> listLoras();

    /**
     * Check if the provider is healthy and ready.
     *
     * @return true if ready to serve requests
     */
    Mono<Boolean> isHealthy();
}
