package com.ai.image.domain.vo;

import java.util.List;

public record ImageCatalog(List<String> models, List<String> sizes, List<String> qualities) {

    private static final ImageCatalog DEFAULT = new ImageCatalog(
            List.of("dall-e-3", "dall-e-2"),
            List.of("1024x1024", "1024x1792", "1792x1024"),
            List.of("standard", "hd"));

    public ImageCatalog {
        if (models == null || models.isEmpty()) {
            throw new IllegalArgumentException("Models list must not be null or empty");
        }
        if (sizes == null || sizes.isEmpty()) {
            throw new IllegalArgumentException("Sizes list must not be null or empty");
        }
        if (qualities == null || qualities.isEmpty()) {
            throw new IllegalArgumentException("Qualities list must not be null or empty");
        }
        models = List.copyOf(models);
        sizes = List.copyOf(sizes);
        qualities = List.copyOf(qualities);
    }

    public static ImageCatalog defaults() {
        return DEFAULT;
    }

    public boolean supportsModel(String model) {
        return models.contains(model);
    }

    public boolean supportsQuality(String quality) {
        return qualities.contains(quality);
    }

    public boolean supportsSize(int width, int height) {
        return sizes.contains(width + "x" + height);
    }

    public String defaultModel() {
        return models.getFirst();
    }

    public String defaultQuality() {
        return qualities.getFirst();
    }
}
