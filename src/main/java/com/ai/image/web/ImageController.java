package com.ai.image.web;

import com.ai.image.application.usecase.ImageFacade;
import com.ai.image.domain.exception.ImageProviderNotConfiguredException;
import com.ai.image.domain.exception.InvalidImagePromptException;
import com.ai.image.domain.model.GeneratedImage;
import com.ai.image.web.dto.ImageGenerationRequest;
import com.ai.image.web.dto.ImageGenerationResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Image generation REST Controller.
 */
@RestController
@RequestMapping("/api")
public class ImageController {

    private static final Logger log = LoggerFactory.getLogger(ImageController.class);

    private final ImageFacade imageFacade;

    public ImageController(ImageFacade imageFacade) {
        this.imageFacade = imageFacade;
    }

    /**
     * Generate an image from text prompt.
     */
    @PostMapping("/images/generate")
    public ResponseEntity<ImageGenerationResponse> generateImage(
            @Valid @RequestBody ImageGenerationRequest request) {
        try {
            GeneratedImage image = imageFacade.generateImage(
                    request.prompt(),
                    request.model(),
                    request.quality(),
                    request.width() != null ? request.width() : 1024,
                    request.height() != null ? request.height() : 1024,
                    request.n() != null ? request.n() : 1);

            if (!image.isAvailable()) {
                return ResponseEntity.internalServerError()
                        .body(ImageGenerationResponse.error("Failed to generate image"));
            }

            String model = request.model() != null ? request.model() : image.model();
            return ResponseEntity.ok(ImageGenerationResponse.success(
                    image.url(), image.base64(), model, request.prompt()));
        } catch (InvalidImagePromptException e) {
            return ResponseEntity.badRequest()
                    .body(ImageGenerationResponse.error(e.getMessage()));
        } catch (ImageProviderNotConfiguredException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ImageGenerationResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error generating image", e);
            return ResponseEntity.internalServerError()
                    .body(ImageGenerationResponse.error("生成图片时发生错误，请稍后重试。"));
        }
    }

    /**
     * Get available image generation models.
     */
    @GetMapping("/images/models")
    public ResponseEntity<Map<String, List<String>>> getImageModels() {
        return ResponseEntity.ok(Map.of("models", imageFacade.getAvailableImageModels()));
    }

    /**
     * Get available image sizes.
     */
    @GetMapping("/images/sizes")
    public ResponseEntity<Map<String, List<String>>> getImageSizes() {
        return ResponseEntity.ok(Map.of("sizes", imageFacade.getAvailableImageSizes()));
    }

    /**
     * Get available image qualities.
     */
    @GetMapping("/images/qualities")
    public ResponseEntity<Map<String, List<String>>> getImageQualities() {
        return ResponseEntity.ok(Map.of("qualities", imageFacade.getAvailableImageQualities()));
    }
}
