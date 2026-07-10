package com.ai.audio.domain.vo;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public record VoiceCatalog(List<String> voices, List<String> models) {

    private static final Map<String, VoiceInfo> VOICE_DETAILS = Map.of(
            "alloy", new VoiceInfo("alloy", "Alloy", "en", "neutral"),
            "echo", new VoiceInfo("echo", "Echo", "en", "male"),
            "fable", new VoiceInfo("fable", "Fable", "en", "male"),
            "onyx", new VoiceInfo("onyx", "Onyx", "en", "male"),
            "nova", new VoiceInfo("nova", "Nova", "en", "female"),
            "shimmer", new VoiceInfo("shimmer", "Shimmer", "en", "female"));

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

    public List<VoiceInfo> voiceInfos() {
        return voices.stream().map(this::toVoiceInfo).toList();
    }

    private VoiceInfo toVoiceInfo(String voiceId) {
        VoiceInfo known = VOICE_DETAILS.get(voiceId);
        if (known != null) {
            return known;
        }
        return new VoiceInfo(voiceId, capitalize(voiceId), "en", "neutral");
    }

    private static String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }
}
