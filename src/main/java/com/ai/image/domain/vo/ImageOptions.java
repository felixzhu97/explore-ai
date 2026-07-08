package com.ai.image.domain.vo;

import com.ai.image.domain.exception.InvalidImagePromptException;

public record ImageOptions(String model, String quality, ImageSize size, int count) {

    public static ImageOptions of(
            String model, String quality, int width, int height, int count) {
        ImageCatalog catalog = ImageCatalog.defaults();
        String effectiveModel = model != null && !model.isBlank() ? model.trim() : catalog.defaultModel();
        String effectiveQuality =
                quality != null && !quality.isBlank() ? quality.trim() : catalog.defaultQuality();
        int effectiveCount = count > 0 ? count : 1;

        if (!catalog.supportsModel(effectiveModel)) {
            throw new InvalidImagePromptException("Unsupported model: " + effectiveModel);
        }
        if (!catalog.supportsQuality(effectiveQuality)) {
            throw new InvalidImagePromptException("Unsupported quality: " + effectiveQuality);
        }
        if (effectiveCount < 1 || effectiveCount > 4) {
            throw new InvalidImagePromptException("Image count must be between 1 and 4");
        }

        return new ImageOptions(
                effectiveModel, effectiveQuality, ImageSize.of(width, height), effectiveCount);
    }
}
