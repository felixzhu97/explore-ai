package com.ai.tts.domain.model;

import java.util.List;

public record ProviderInfo(
    String name,
    String displayName,
    List<String> supportedLanguages,
    List<String> features
) {
    public static ProviderInfo of(String name, String displayName, List<String> languages, List<String> features) {
        return new ProviderInfo(name, displayName, languages, features);
    }
}
