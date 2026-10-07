package com.ai.image.service;

import com.ai.common.exception.DomainException;
import com.ai.image.domain.model.GeneratedImage;
import com.ai.image.domain.model.ImageCatalog;
import com.ai.image.domain.model.ImageOptions;
import com.ai.image.domain.model.ImagePrompt;
import com.ai.image.domain.repository.ImageGenerationGateway;
import com.ai.image.infra.config.ImageProperties;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Entry point for image generation and the catalog of supported models, sizes, and qualities. */
@Service
@RequiredArgsConstructor
public class ImageGenerationService {

  private final ImageGenerationGateway imageGenerationGateway;
  private final ImageProperties imageProperties;

  /** Lists the supported image models. */
  public List<String> getAvailableImageModels() {
    return ImageCatalog.createDefaultCatalog().getModels();
  }

  /** Lists the supported image sizes. */
  public List<String> getAvailableImageSizes() {
    return ImageCatalog.createDefaultCatalog().getSizes();
  }

  /** Lists the supported image qualities. */
  public List<String> getAvailableImageQualities() {
    return ImageCatalog.createDefaultCatalog().getQualities();
  }

  /** Generates an image after checking the provider is configured, or returns an empty image. */
  public GeneratedImage generateImage(
      String prompt, String model, String quality, int width, int height, int n) {
    ensureProviderConfigured();
    GeneratedImage image =
        imageGenerationGateway.generateImage(
            ImagePrompt.createPrompt(prompt),
            ImageOptions.createOptions(resolveModel(model), quality, width, height, n));
    return image.isAvailable() ? image : GeneratedImage.createEmptyImage();
  }

  private void ensureProviderConfigured() {
    if (!imageProperties.isEnabled()) {
      throw DomainException.unavailable(
          "IMAGE_PROVIDER_NOT_CONFIGURED", "Image generation is disabled");
    }
    if (!imageProperties.isConfigured()) {
      if (imageProperties.isOpenAiProvider()) {
        throw DomainException.unavailable(
            "IMAGE_PROVIDER_NOT_CONFIGURED",
            "Image provider not configured. Set OPENAI_API_KEY and app.ai.image.provider=openai");
      }
      throw DomainException.unavailable(
          "IMAGE_PROVIDER_NOT_CONFIGURED",
          "Image provider not configured. Run: ollama pull x/flux2-klein");
    }
    if (imageProperties.isOllamaProvider() && isLocalOllamaEndpoint(imageProperties.getBaseUrl())) {
      throw DomainException.unavailable(
          "IMAGE_PROVIDER_NOT_CONFIGURED",
          "Image provider not configured. Run: ollama pull x/flux2-klein");
    }
  }

  private boolean isLocalOllamaEndpoint(String baseUrl) {
    if (!StringUtils.hasText(baseUrl)) {
      return true;
    }
    String normalized = baseUrl.toLowerCase();
    return normalized.contains("localhost") || normalized.contains("127.0.0.1");
  }

  private String resolveModel(String model) {
    if (StringUtils.hasText(model)) {
      return model.trim();
    }
    return imageProperties.getModel();
  }
}
