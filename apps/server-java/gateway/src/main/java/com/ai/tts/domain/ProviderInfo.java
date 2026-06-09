package com.ai.tts.domain;

import java.util.List;

public record ProviderInfo(
    String name,
    String displayName,
    List<String> supportedLanguages,
    List<String> features,
    boolean isActive
) {
    public static ProviderInfo of(String name, String displayName, List<String> languages, List<String> features) {
        return new ProviderInfo(name, displayName, languages, features, false);
    }

    public static ProviderInfo active(String name, String displayName, List<String> languages, List<String> features) {
        return new ProviderInfo(name, displayName, languages, features, true);
    }
}
