package com.ai.image.domain.model;

import com.ai.common.exception.DomainException;

public record ImageOptions(String model, String quality, ImageSize size, int count) {
  /** Creates options with catalog defaults, validating model, quality, size, and count (1-4). */
  public static ImageOptions createOptions(
      String model, String quality, int width, int height, int count) {
    ImageCatalog catalog = ImageCatalog.createDefaultCatalog();
    String effectiveModel =
        model != null && !model.isBlank() ? model.trim() : catalog.getDefaultModel();
    String effectiveQuality =
        quality != null && !quality.isBlank() ? quality.trim() : catalog.getDefaultQuality();
    int effectiveCount = count > 0 ? count : 1;

    if (!catalog.supportsModel(effectiveModel)) {
      throw DomainException.invalid("INVALID_IMAGE_PROMPT", "Unsupported model: " + effectiveModel);
    }
    if (!catalog.supportsQuality(effectiveQuality)) {
      throw DomainException.invalid(
          "INVALID_IMAGE_PROMPT", "Unsupported quality: " + effectiveQuality);
    }
    if (effectiveCount < 1 || effectiveCount > 4) {
      throw DomainException.invalid("INVALID_IMAGE_PROMPT", "Image count must be between 1 and 4");
    }

    return new ImageOptions(
        effectiveModel, effectiveQuality, ImageSize.createSize(width, height), effectiveCount);
  }
}
