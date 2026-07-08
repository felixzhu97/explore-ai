package com.ai.audio.domain.vo;

import com.ai.audio.domain.exception.InvalidSpeechTextException;

import java.util.List;

public record VoiceSelection(String voice, String model) {

    public static VoiceSelection of(String voice, String model) {
        VoiceCatalog catalog = VoiceCatalog.defaults();
        String effectiveVoice = voice != null && !voice.isBlank() ? voice.trim() : catalog.defaultVoice();
        String effectiveModel = model != null && !model.isBlank() ? model.trim() : catalog.defaultModel();
        if (!catalog.containsVoice(effectiveVoice)) {
            throw new InvalidSpeechTextException("Unknown voice: " + effectiveVoice);
        }
        if (!catalog.containsModel(effectiveModel)) {
            throw new InvalidSpeechTextException("Unknown model: " + effectiveModel);
        }
        return new VoiceSelection(effectiveVoice, effectiveModel);
    }

    public boolean isDefault() {
        VoiceCatalog catalog = VoiceCatalog.defaults();
        return catalog.defaultVoice().equals(voice) && catalog.defaultModel().equals(model);
    }
}
