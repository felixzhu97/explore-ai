package com.ai.image.controller.dto;

/** Response DTO for image generation. */
public record ImageGenerationResponse(
    String imageUrl,
    String imageBase64,
    String model,
    String prompt,
    String revisedPrompt,
    String status) {
  /** Documentation. */
  public static ImageGenerationResponse success(
      String imageUrl, String imageBase64, String model, String prompt) {
    return new ImageGenerationResponse(imageUrl, imageBase64, model, prompt, null, "SUCCESS");
  }

  /** Documentation. */
  public static ImageGenerationResponse error(String message) {
    return new ImageGenerationResponse(null, null, null, null, null, "ERROR: " + message);
  }
}
