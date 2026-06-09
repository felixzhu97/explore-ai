package com.ai.vision.application.service;

import com.ai.vision.domain.*;
import com.ai.vision.domain.exception.VisionException;
import com.ai.vision.presentation.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class VisionApplicationService {

    private static final Logger log = LoggerFactory.getLogger(VisionApplicationService.class);

    private final Map<ModelType, VisionModel> providers;
    private final Map<String, VideoTaskState> videoTasks = new ConcurrentHashMap<>();

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

    // ========================================================================
    // Vision Endpoints
    // ========================================================================

    public Mono<DetectionResponse> detectObjects(byte[] imageData, float confidence) {
        return Mono.defer(() -> {
            ImageData data = ImageData.of(imageData);
            Image image = Image.create(data);
            long startTime = System.currentTimeMillis();
            
            image.beginDetection(confidence);
            
            VisionModel provider = getProvider(ModelType.YOLO);
            return provider.detect(data, confidence)
                .doOnNext(result -> image.completeDetection(result))
                .doOnError(e -> image.failDetection(e.getMessage()))
                .map(result -> {
                    double processingTimeMs = System.currentTimeMillis() - startTime;
                    return toDetectionResponse(result, data.width(), data.height(), processingTimeMs);
                })
                .doOnSuccess(r -> log.info("Detection completed: {} objects found in {}ms", 
                    r.objects().size(), r.processingTimeMs()))
                .doOnError(e -> log.error("Detection failed: {}", e.getMessage()));
        });
    }

    public Mono<CaptionResponse> captionImage(byte[] imageData) {
        return Mono.defer(() -> {
            ImageData data = ImageData.of(imageData);
            Image image = Image.create(data);
            long startTime = System.currentTimeMillis();
            
            image.beginCaptioning();
            
            VisionModel provider = getProvider(ModelType.BLIP);
            return provider.caption(data)
                .doOnNext(result -> image.completeCaptioning(result))
                .doOnError(e -> image.failCaptioning(e.getMessage()))
                .map(result -> {
                    double processingTimeMs = System.currentTimeMillis() - startTime;
                    return CaptionResponse.of("blip", result.caption(), processingTimeMs);
                })
                .doOnSuccess(r -> log.info("Captioning completed in {}ms", r.processingTimeMs()))
                .doOnError(e -> log.error("Captioning failed: {}", e.getMessage()));
        });
    }

    public Mono<OcrResponse> recognizeText(byte[] imageData, String language) {
        return Mono.defer(() -> {
            ImageData data = ImageData.of(imageData);
            Image image = Image.create(data);
            long startTime = System.currentTimeMillis();
            
            image.beginOcr(language);
            
            VisionModel provider = getProvider(ModelType.OCR);
            return provider.recognizeText(data, language)
                .doOnNext(result -> image.completeOcr(result))
                .doOnError(e -> image.failOcr(e.getMessage()))
                .map(result -> {
                    double processingTimeMs = System.currentTimeMillis() - startTime;
                    List<OcrResponse.TextBlock> blocks = result.blocks().stream()
                        .map(block -> new OcrResponse.TextBlock(
                            block.text(),
                            block.confidence(),
                            block.bbox() != null ? List.of(
                                List.of(block.bbox().x1(), block.bbox().y1()),
                                List.of(block.bbox().x2(), block.bbox().y1()),
                                List.of(block.bbox().x2(), block.bbox().y2()),
                                List.of(block.bbox().x1(), block.bbox().y2())
                            ) : null
                        ))
                        .toList();
                    return OcrResponse.of("easyocr", blocks, processingTimeMs);
                })
                .doOnSuccess(r -> log.info("OCR completed: {} chars in {}ms", r.fullText().length(), r.processingTimeMs()))
                .doOnError(e -> log.error("OCR failed: {}", e.getMessage()));
        });
    }

    // ========================================================================
    // Image Generation Endpoints
    // ========================================================================

    public Mono<GenerateResponse> generateImage(GenerateRequest request) {
        return Mono.defer(() -> {
            VisionModel provider = getProvider(ModelType.STABLE_DIFFUSION);
            long startTime = System.currentTimeMillis();
            
            GenerateParams params = GenerateParams.fromPrompt(request.prompt());
            
            return provider.generate(params)
                .doOnNext(result -> log.info("Image generation completed: {}", result.imageUrl()))
                .doOnError(e -> log.error("Generation failed: {}", e.getMessage()))
                .map(result -> {
                    double processingTimeMs = System.currentTimeMillis() - startTime;
                    return GenerateResponse.of(
                        List.of(result.base64Image() != null ? result.base64Image() : result.imageUrl()),
                        result.seed(),
                        "stable-diffusion-xl",
                        request.prompt(),
                        request.steps(),
                        request.guidanceScale(),
                        request.width(),
                        request.height(),
                        processingTimeMs
                    );
                });
        });
    }

    public Mono<Map<String, Object>> generateVariation(VariationRequest request) {
        return Mono.defer(() -> {
            long startTime = System.currentTimeMillis();
            VisionModel provider = getProvider(ModelType.STABLE_DIFFUSION);
            
            log.info("Variation generation requested for image with strength={}", request.strength());
            
            return Mono.just(Map.<String, Object>of(
                "images", List.of(request.image()),
                "seed", request.seed() != null ? request.seed() : (int)(Math.random() * Integer.MAX_VALUE),
                "prompt", request.prompt(),
                "strength", request.strength(),
                "inference_steps", request.numInferenceSteps(),
                "processing_time_ms", System.currentTimeMillis() - startTime
            ));
        });
    }

    public Mono<Map<String, Object>> upscaleImage(UpscaleRequest request) {
        return Mono.defer(() -> {
            long startTime = System.currentTimeMillis();
            
            log.info("Upscale requested with scale={}", request.scale());
            
            return Mono.just(Map.<String, Object>of(
                "image", request.image(),
                "scale", request.scale(),
                "original_width", 512,
                "original_height", 512,
                "new_width", 512 * request.scale(),
                "new_height", 512 * request.scale(),
                "processing_time_ms", System.currentTimeMillis() - startTime
            ));
        });
    }

    // ========================================================================
    // Video Generation Endpoints
    // ========================================================================

    public Mono<VideoGenerateResponse> generateVideo(VideoGenerateRequest request) {
        return Mono.defer(() -> {
            String taskId = UUID.randomUUID().toString();
            Instant createdAt = Instant.now();
            
            videoTasks.put(taskId, new VideoTaskState(taskId, "pending", createdAt));
            
            log.info("Video generation task created: {} with prompt: {}", taskId, request.prompt());
            
            return Mono.just(new VideoGenerateResponse(
                taskId,
                "pending",
                "Video generation task submitted",
                createdAt.toString()
            ));
        });
    }

    public Mono<VideoGenerateResponse> generateVideoAdvanced(VideoGenerateRequest request) {
        return generateVideo(request);
    }

    public Mono<VideoStatusResponse> getVideoStatus(String taskId) {
        return Mono.defer(() -> {
            VideoTaskState task = videoTasks.get(taskId);
            if (task == null) {
                return Mono.error(new IllegalArgumentException("Task not found: " + taskId));
            }
            return Mono.just(new VideoStatusResponse(
                task.taskId,
                task.status,
                task.status.equals("completed") ? "https://example.com/video/" + taskId + ".mp4" : null,
                task.status.equals("completed") ? "https://example.com/video/" + taskId + "_thumb.jpg" : null,
                task.status.equals("failed") ? "Generation failed" : null,
                null,
                null
            ));
        });
    }

    // ========================================================================
    // Combined Analysis
    // ========================================================================

    public Mono<Map<String, Object>> analyzeImage(byte[] imageData, String task) {
        return Mono.defer(() -> {
            ImageData data = ImageData.of(imageData);
            long startTime = System.currentTimeMillis();
            
            VisionTask vt = VisionTask.fromString(task);
            return switch (vt) {
                case DETECT -> getProvider(ModelType.YOLO)
                    .detect(data, 0.25f)
                    .map(r -> {
                        double processingTimeMs = System.currentTimeMillis() - startTime;
                        return Map.<String, Object>of(
                            "detections", toDetectionResponse(r, data.width(), data.height(), processingTimeMs)
                        );
                    });
                case CAPTION -> getProvider(ModelType.BLIP)
                    .caption(data)
                    .map(r -> {
                        double processingTimeMs = System.currentTimeMillis() - startTime;
                        return Map.<String, Object>of(
                            "caption", CaptionResponse.of("blip", r.caption(), processingTimeMs)
                        );
                    });
                case OCR -> getProvider(ModelType.OCR)
                    .recognizeText(data, "eng")
                    .map(r -> {
                        double processingTimeMs = System.currentTimeMillis() - startTime;
                        List<OcrResponse.TextBlock> blocks = r.blocks().stream()
                            .map(block -> new OcrResponse.TextBlock(
                                block.text(),
                                block.confidence(),
                                block.bbox() != null ? List.of(
                                    List.of(block.bbox().x1(), block.bbox().y1()),
                                    List.of(block.bbox().x2(), block.bbox().y1()),
                                    List.of(block.bbox().x2(), block.bbox().y2()),
                                    List.of(block.bbox().x1(), block.bbox().y2())
                                ) : null
                            ))
                            .toList();
                        return Map.<String, Object>of(
                            "ocr", OcrResponse.of("easyocr", blocks, processingTimeMs)
                        );
                    });
                case GENERATE -> Mono.error(
                    new IllegalArgumentException("GENERATE task not supported in analyze mode"));
            };
        });
    }

    // ========================================================================
    // Provider Management
    // ========================================================================

    public Map<ModelType, Boolean> getProviderStatus() {
        return providers.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().isAvailable()
            ));
    }

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

    // ========================================================================
    // DTO Mapping
    // ========================================================================

    private DetectionResponse toDetectionResponse(DetectionResult result, int width, int height, double processingTimeMs) {
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
        return DetectionResponse.of("yolov8", objects, width, height, processingTimeMs);
    }

    // ========================================================================
    // Internal Types
    // ========================================================================

    private record VideoTaskState(String taskId, String status, Instant createdAt) {}
}
