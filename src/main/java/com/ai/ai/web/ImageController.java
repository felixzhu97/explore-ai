package com.ai.ai.web;

import com.ai.ai.web.dto.ImageGenerationRequest;
import com.ai.ai.web.dto.ImageGenerationResponse;
import com.ai.ai.application.usecase.ImageGenerationUseCasePort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Image Generation operations.
 */
@RestController
@RequestMapping("/api/images")
@Tag(name = "Image Generation", description = "Generate images using DALL-E")
public class ImageController {

    private static final Logger log = LoggerFactory.getLogger(ImageController.class);

    private final ImageGenerationUseCasePort imageGenerationUseCase;

    public ImageController(ImageGenerationUseCasePort imageGenerationUseCase) {
        this.imageGenerationUseCase = imageGenerationUseCase;
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate an image from text prompt")
    public ResponseEntity<ImageGenerationResponse> generateImage(
            @Valid @RequestBody ImageGenerationRequest request) {
        try {
            String imageUrl = imageGenerationUseCase.generateImage(
                    request.prompt(),
                    request.model(),
                    request.quality(),
                    request.width() != null ? request.width() : 1024,
                    request.height() != null ? request.height() : 1024,
                    request.n() != null ? request.n() : 1
            );

            if (imageUrl == null) {
                return ResponseEntity.internalServerError()
                        .body(ImageGenerationResponse.error("Failed to generate image"));
            }

            return ResponseEntity.ok(ImageGenerationResponse.success(
                    imageUrl,
                    request.model() != null ? request.model() : "dall-e-3",
                    request.prompt()
            ));
        } catch (Exception e) {
            log.error("Error generating image", e);
            return ResponseEntity.internalServerError()
                    .body(ImageGenerationResponse.error("生成图片时发生错误，请稍后重试。"));
        }
    }

    @GetMapping("/models")
    @Operation(summary = "Get available image generation models")
    public ResponseEntity<Map<String, List<String>>> getModels() {
        return ResponseEntity.ok(Map.of(
                "models", imageGenerationUseCase.getAvailableModels()
        ));
    }

    @GetMapping("/sizes")
    @Operation(summary = "Get available image sizes")
    public ResponseEntity<Map<String, List<String>>> getSizes() {
        return ResponseEntity.ok(Map.of(
                "sizes", imageGenerationUseCase.getAvailableSizes()
        ));
    }

    @GetMapping("/qualities")
    @Operation(summary = "Get available quality options")
    public ResponseEntity<Map<String, List<String>>> getQualities() {
        return ResponseEntity.ok(Map.of(
                "qualities", imageGenerationUseCase.getAvailableQualities()
        ));
    }
}
