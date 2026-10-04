package com.ai.image.controller;

import com.ai.common.domain.exception.AiServiceException;
import com.ai.image.controller.dto.ImageGenerationRequest;
import com.ai.image.controller.dto.ImageGenerationResponse;
import com.ai.image.domain.model.GeneratedImage;
import com.ai.image.service.ImageGenerationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Image generation REST Controller. */
@RestController
@RequestMapping("/api/images")
public class ImageController {

  private final ImageGenerationService imageGenerationService;

  public ImageController(ImageGenerationService imageGenerationService) {
    this.imageGenerationService = imageGenerationService;
  }

  /** Generate an image from text prompt. */
  @PostMapping("/generate")
  public ImageGenerationResponse generateImage(@Valid @RequestBody ImageGenerationRequest request) {
    GeneratedImage image =
        imageGenerationService.generateImage(
            request.prompt(),
            request.model(),
            request.quality(),
            request.width(),
            request.height(),
            request.n());
    if (!image.isAvailable()) {
      throw new AiServiceException("Failed to generate image", "IMAGE_GENERATION_FAILED", null);
    }
    String model = request.model() != null ? request.model() : image.model();
    return ImageGenerationResponse.success(image.url(), image.base64(), model, request.prompt());
  }

  /** Get available image generation models. */
  @GetMapping("/models")
  public ResponseEntity<Map<String, List<String>>> getImageModels() {
    return ResponseEntity.ok(Map.of("models", imageGenerationService.getAvailableImageModels()));
  }

  /** Get available image sizes. */
  @GetMapping("/sizes")
  public ResponseEntity<Map<String, List<String>>> getImageSizes() {
    return ResponseEntity.ok(Map.of("sizes", imageGenerationService.getAvailableImageSizes()));
  }

  /** Get available image qualities. */
  @GetMapping("/qualities")
  public ResponseEntity<Map<String, List<String>>> getImageQualities() {
    return ResponseEntity.ok(
        Map.of("qualities", imageGenerationService.getAvailableImageQualities()));
  }
}
