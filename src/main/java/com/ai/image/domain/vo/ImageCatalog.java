package com.ai.image.domain.vo;

import java.util.List;

public record ImageCatalog(List<String> models, List<String> sizes, List<String> qualities) {

    public static ImageCatalog defaults() {
        return new ImageCatalog(
                List.of("dall-e-3", "dall-e-2"),
                List.of("1024x1024", "1024x1792", "1792x1024"),
                List.of("standard", "hd"));
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
