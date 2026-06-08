package com.ai.tts.domain.model;

public record Voice(
    String id,
    String name,
    String language,
    String languageName,
    String gender,
    String provider,
    boolean isDefault
) {
    public static Voice of(String id, String name, String language, String provider) {
        return new Voice(id, name, language, null, null, provider, false);
    }

    public static Voice defaultVoice(String id, String name, String language, String provider) {
        return new Voice(id, name, language, null, null, provider, true);
    }
}
