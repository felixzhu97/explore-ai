package com.ai.image.controller.dto;

/** Response DTO for image generation. */
public record ImageGenerationResponse(
    String imageUrl,
    String imageBase64,
    String model,
    String prompt,
    String revisedPrompt,
    ImageGenerationStatus status) {
  /** Creates a successful response. */
  public static ImageGenerationResponse createSuccessResponse(
      String imageUrl, String imageBase64, String model, String prompt) {
    return new ImageGenerationResponse(
        imageUrl, imageBase64, model, prompt, null, ImageGenerationStatus.SUCCESS);
  }
}
