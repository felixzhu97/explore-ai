package com.ai.media.model;

public record LoraInfo(
        String id,
        String name,
        String description,
        String triggerWord,
        boolean isInstalled
) {
    public static LoraInfo of(String id, String name) {
        return new LoraInfo(id, name, null, null, true);
    }

    public static LoraInfo of(String id, String name, boolean isInstalled) {
        return new LoraInfo(id, name, null, null, isInstalled);
    }
}
