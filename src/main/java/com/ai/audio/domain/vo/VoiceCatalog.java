package com.ai.audio.domain.vo;

import java.util.List;

public record VoiceCatalog(List<String> voices, List<String> models) {

    public VoiceCatalog {
        if (voices == null || voices.isEmpty()) {
            throw new IllegalArgumentException("Voices list must not be null or empty");
        }
        if (models == null || models.isEmpty()) {
            throw new IllegalArgumentException("Models list must not be null or empty");
        }
        voices = List.copyOf(voices);
        models = List.copyOf(models);
    }

    public static VoiceCatalog defaults() {
        return new VoiceCatalog(
                List.of("alloy", "echo", "fable", "onyx", "nova", "shimmer"),
                List.of("gpt-4o-mini-tts", "gpt-4o-tts", "tts-1", "tts-1-hd"));
    }

    public boolean containsVoice(String voice) {
        return voices.contains(voice);
    }

    public boolean containsModel(String model) {
        return models.contains(model);
    }

    public String defaultVoice() {
        return voices.getFirst();
    }

    public String defaultModel() {
        return models.getFirst();
    }
}
