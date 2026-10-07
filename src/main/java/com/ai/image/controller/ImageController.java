package com.ai.image.controller;

import com.ai.common.exception.DomainException;
import com.ai.image.controller.dto.ImageGenerationRequest;
import com.ai.image.controller.dto.ImageGenerationResponse;
import com.ai.image.controller.dto.ImageModelsResponse;
import com.ai.image.controller.dto.ImageQualitiesResponse;
import com.ai.image.controller.dto.ImageSizesResponse;
import com.ai.image.domain.model.GeneratedImage;
import com.ai.image.service.ImageGenerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Image generation REST Controller. */
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

  private final ImageGenerationService imageGenerationService;

  /** Get available image generation models. */
  @GetMapping("/models")
  public ResponseEntity<ImageModelsResponse> getImageModels() {
    return ResponseEntity.ok(
        new ImageModelsResponse(imageGenerationService.getAvailableImageModels()));
  }

  /** Get available image sizes. */
  @GetMapping("/sizes")
  public ResponseEntity<ImageSizesResponse> getImageSizes() {
    return ResponseEntity.ok(
        new ImageSizesResponse(imageGenerationService.getAvailableImageSizes()));
  }

  /** Get available image qualities. */
  @GetMapping("/qualities")
  public ResponseEntity<ImageQualitiesResponse> getImageQualities() {
    return ResponseEntity.ok(
        new ImageQualitiesResponse(imageGenerationService.getAvailableImageQualities()));
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
      throw DomainException.createUnavailableError(
          "IMAGE_GENERATION_FAILED", "Failed to generate image");
    }
    String model = request.model() != null ? request.model() : image.getModel();
    return ImageGenerationResponse.createSuccessResponse(
        image.getUrl(), image.getBase64(), model, request.prompt());
  }
}
