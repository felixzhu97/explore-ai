package com.ai.image.domain.vo;

import com.ai.image.domain.exception.InvalidImagePromptException;

public record ImageSize(int width, int height) {

    public static ImageSize of(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new InvalidImagePromptException("Image dimensions must be positive");
        }
        ImageSize size = new ImageSize(width, height);
        if (!size.isSupported()) {
            throw new InvalidImagePromptException("Unsupported image size: " + width + "x" + height);
        }
        return size;
    }

    public boolean isSupported() {
        return ImageCatalog.defaults().supportsSize(width, height);
    }
}
