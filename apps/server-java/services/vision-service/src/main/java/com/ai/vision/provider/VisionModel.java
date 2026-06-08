package com.ai.vision.provider;

import com.ai.vision.model.*;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.nio.file.Path;
import java.util.List;

/**
 * Unified Vision Model Provider interface.
 * Abstracts different vision models (YOLO, BLIP, OCR, Stable Diffusion).
 */
public interface VisionModel {

    /**
     * Model type identifier.
     */
    ModelType type();

    /**
     * Initialize the model. Called once at startup.
     */
    default Mono<Void> initialize() {
        return Mono.empty();
    }

    /**
     * Check if the model is loaded and ready.
     */
    boolean isAvailable();

    /**
     * Object detection using YOLO.
     */
    default Mono<DetectionResponse> detect(byte[] imageData, float confidence) {
        return Mono.error(new UnsupportedOperationException(
            "detect not supported by " + type()));
    }

    /**
     * Image captioning using BLIP.
     */
    default Mono<CaptionResponse> caption(byte[] imageData) {
        return Mono.error(new UnsupportedOperationException(
            "caption not supported by " + type()));
    }

    /**
     * OCR text recognition.
     */
    default Mono<OcrResponse> recognizeText(byte[] imageData, String language) {
        return Mono.error(new UnsupportedOperationException(
            "recognizeText not supported by " + type()));
    }

    /**
     * Image generation using Stable Diffusion.
     */
    default Mono<GenerateResponse> generate(GenerateRequest request) {
        return Mono.error(new UnsupportedOperationException(
            "generate not supported by " + type()));
    }

    /**
     * Reactive version for streaming large images.
     */
    default Flux<byte[]> detectStream(Flux<byte[]> imageChunks, float confidence) {
        return Flux.error(new UnsupportedOperationException(
            "detectStream not supported by " + type()));
    }

    enum ModelType {
        YOLO,
        BLIP,
        OCR,
        STABLE_DIFFUSION
    }
}
