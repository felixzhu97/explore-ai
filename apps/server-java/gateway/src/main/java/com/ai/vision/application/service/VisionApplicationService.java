package com.ai.vision.application.service;

import com.ai.vision.domain.*;
import com.ai.vision.domain.exception.VisionException;
import com.ai.vision.presentation.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Application Service for Vision operations.
 * 
 * Acts as a transaction script that:
 * 1. Orchestrates domain objects and providers
 * 2. Handles request/response DTOs
 * 3. Contains no business logic (delegates to domain)
 * 
 * This is the entry point for the presentation layer.
 */
@Service
public class VisionApplicationService {

    private static final Logger log = LoggerFactory.getLogger(VisionApplicationService.class);

    private final Map<ModelType, VisionModel> providers;

    public VisionApplicationService(List<VisionModel> visionModels) {
        if (visionModels == null || visionModels.isEmpty()) {
            throw new IllegalArgumentException("At least one VisionModel provider is required");
        }
        this.providers = visionModels.stream()
            .collect(Collectors.toMap(
                VisionModel::type,
                Function.identity()
            ));
        
        log.info("VisionApplicationService initialized with {} providers: {}", 
                 providers.size(), providers.keySet());
    }

    /**
     * Perform object detection on an image.
     * 
     * @param imageData Raw image bytes
     * @param confidence Confidence threshold (0.0-1.0)
     * @return Detection response
     */
    public Mono<DetectionResponse> detectObjects(byte[] imageData, float confidence) {
        return Mono.defer(() -> {
            ImageData data = ImageData.of(imageData);
            Image image = Image.create(data);
            
            image.beginDetection(confidence);
            
            VisionModel provider = getProvider(ModelType.YOLO);
            return provider.detect(data, confidence)
                .doOnNext(result -> image.completeDetection(result))
                .doOnError(e -> image.failDetection(e.getMessage()))
                .map(this::toDetectionResponse)
                .doOnSuccess(r -> log.info("Detection completed: {} objects found", 
                    r.objects().size()))
                .doOnError(e -> log.error("Detection failed: {}", e.getMessage()));
        });
    }

    /**
     * Perform image captioning.
     * 
     * @param imageData Raw image bytes
     * @return Caption response
     */
    public Mono<CaptionResponse> captionImage(byte[] imageData) {
        return Mono.defer(() -> {
            ImageData data = ImageData.of(imageData);
            Image image = Image.create(data);
            
            image.beginCaptioning();
            
            VisionModel provider = getProvider(ModelType.BLIP);
            return provider.caption(data)
                .doOnNext(result -> image.completeCaptioning(result))
                .doOnError(e -> image.failCaptioning(e.getMessage()))
                .map(this::toCaptionResponse)
                .doOnSuccess(r -> log.info("Captioning completed: {}", r.caption()))
                .doOnError(e -> log.error("Captioning failed: {}", e.getMessage()));
        });
    }

    /**
     * Perform OCR text recognition.
     * 
     * @param imageData Raw image bytes
     * @param language Language code (e.g., "eng", "chi_sim")
     * @return OCR response
     */
    public Mono<OcrResponse> recognizeText(byte[] imageData, String language) {
        return Mono.defer(() -> {
            ImageData data = ImageData.of(imageData);
            Image image = Image.create(data);
            
            image.beginOcr(language);
            
            VisionModel provider = getProvider(ModelType.OCR);
            return provider.recognizeText(data, language)
                .doOnNext(result -> image.completeOcr(result))
                .doOnError(e -> image.failOcr(e.getMessage()))
                .map(this::toOcrResponse)
                .doOnSuccess(r -> log.info("OCR completed: {} characters recognized", r.text().length()))
                .doOnError(e -> log.error("OCR failed: {}", e.getMessage()));
        });
    }

    /**
     * Generate an image from text prompt.
     * 
     * @param request Generation parameters
     * @return Generation response
     */
    public Mono<GenerateResponse> generateImage(GenerateRequest request) {
        return Mono.defer(() -> {
            VisionModel provider = getProvider(ModelType.STABLE_DIFFUSION);
            
            GenerateParams params = GenerateParams.fromPrompt(request.prompt());
            
            return provider.generate(params)
                .doOnNext(result -> log.info("Image generation completed: {}", result.imageUrl()))
                .doOnError(e -> log.error("Generation failed: {}", e.getMessage()))
                .map(this::toGenerateResponse);
        });
    }

    /**
     * Perform combined analysis on an image.
     * 
     * @param imageData Raw image bytes
     * @param tasks List of tasks to perform
     * @return Combined analysis results
     */
    public Mono<Map<String, Object>> analyzeImage(byte[] imageData, List<String> tasks) {
        return Mono.defer(() -> {
            ImageData data = ImageData.of(imageData);
            
            return Mono.when(
                tasks.stream()
                    .map(task -> switch (VisionTask.fromString(task)) {
                        case DETECT -> getProvider(ModelType.YOLO)
                            .detect(data, 0.5f)
                            .map(r -> Map.<String, Object>of("objects", toDetectionResponse(r).objects()));
                        case CAPTION -> getProvider(ModelType.BLIP)
                            .caption(data)
                            .map(r -> Map.<String, Object>of("caption", toCaptionResponse(r).caption()));
                        case OCR -> getProvider(ModelType.OCR)
                            .recognizeText(data, "eng")
                            .map(r -> Map.<String, Object>of("text", toOcrResponse(r).text()));
                        case GENERATE -> Mono.error(
                            new IllegalArgumentException("GENERATE task not supported in analyze mode"));
                    })
                    .toList()
            ).then(Mono.defer(() -> {
                Mono<Map<String, Object>> resultMono = Mono.empty();
                for (String task : tasks) {
                    if (resultMono == Mono.<Map<String, Object>>empty()) {
                        resultMono = executeTask(data, task);
                    } else {
                        final Mono<Map<String, Object>> prevResult = resultMono;
                        resultMono = prevResult.flatMap(map -> 
                            executeTask(data, task).map(result -> {
                                map.putAll(result);
                                return map;
                            })
                        );
                    }
                }
                return resultMono != Mono.<Map<String, Object>>empty() 
                    ? resultMono 
                    : Mono.just(Map.<String, Object>of("status", "no tasks"));
            }));
        });
    }

    private Mono<Map<String, Object>> executeTask(ImageData data, String task) {
        return switch (VisionTask.fromString(task)) {
            case DETECT -> getProvider(ModelType.YOLO)
                .detect(data, 0.5f)
                .map(r -> Map.<String, Object>of("objects", toDetectionResponse(r).objects()));
            case CAPTION -> getProvider(ModelType.BLIP)
                .caption(data)
                .map(r -> Map.<String, Object>of("caption", toCaptionResponse(r).caption()));
            case OCR -> getProvider(ModelType.OCR)
                .recognizeText(data, "eng")
                .map(r -> Map.<String, Object>of("text", toOcrResponse(r).text()));
            case GENERATE -> Mono.error(
                new IllegalArgumentException("GENERATE task not supported in analyze mode"));
        };
    }

    /**
     * Get provider status for all models.
     */
    public Map<ModelType, Boolean> getProviderStatus() {
        return providers.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().isAvailable()
            ));
    }

    /**
     * Check if a specific provider is available.
     */
    public boolean isProviderAvailable(ModelType type) {
        VisionModel provider = providers.get(type);
        return provider != null && provider.isAvailable();
    }

    private VisionModel getProvider(ModelType type) {
        VisionModel provider = providers.get(type);
        if (provider == null) {
            throw VisionException.modelNotAvailable(type.name());
        }
        if (!provider.isAvailable()) {
            throw VisionException.modelNotAvailable(type.name());
        }
        return provider;
    }

    // ============================================================
    // DTO Mapping
    // ============================================================

    private DetectionResponse toDetectionResponse(DetectionResult result) {
        List<DetectionResponse.DetectedObject> objects = result.objects().stream()
            .map(obj -> new DetectionResponse.DetectedObject(
                obj.label(),
                obj.confidence(),
                new DetectionResponse.BoundingBox(
                    obj.bbox().x(),
                    obj.bbox().y(),
                    obj.bbox().width(),
                    obj.bbox().height()
                )
            ))
            .toList();
        return new DetectionResponse(objects);
    }

    private CaptionResponse toCaptionResponse(CaptionResult result) {
        return new CaptionResponse(result.caption());
    }

    private OcrResponse toOcrResponse(OcrResult result) {
        List<OcrResponse.TextBlock> blocks = result.blocks().stream()
            .map(block -> new OcrResponse.TextBlock(
                block.text(),
                block.confidence(),
                block.bbox() != null ? new OcrResponse.BoundingBox(
                    block.bbox().x1(),
                    block.bbox().y1(),
                    block.bbox().x2(),
                    block.bbox().y2()
                ) : null
            ))
            .toList();
        return new OcrResponse(result.text(), result.confidence(), blocks);
    }

    private GenerateResponse toGenerateResponse(GeneratedImage result) {
        return new GenerateResponse(
            result.imageUrl(),
            result.base64Image(),
            result.seed()
        );
    }
}
