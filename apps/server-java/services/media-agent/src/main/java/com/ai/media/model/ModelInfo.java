package com.ai.media.model;

import java.util.List;

public record ModelInfo(
        String id,
        String name,
        String type,
        String description,
        List<String> supportedTasks,
        boolean isDefault
) {
    public static ModelInfo of(String id, String name, String type) {
        return new ModelInfo(id, name, type, null, List.of("text-to-image"), false);
    }

    public static ModelInfo of(String id, String name, String type, boolean isDefault) {
        return new ModelInfo(id, name, type, null, List.of("text-to-image"), isDefault);
    }
}
