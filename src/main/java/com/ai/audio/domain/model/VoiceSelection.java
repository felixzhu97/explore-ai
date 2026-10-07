package com.ai.audio.domain.model;

import com.ai.common.exception.DomainException;

public record VoiceSelection(String voice, String model) {
  /** Creates a selection with catalog defaults, rejecting unknown voices or models. */
  public static VoiceSelection createSelection(String voice, String model) {
    VoiceCatalog catalog = VoiceCatalog.createDefaultCatalog();
    String effectiveVoice =
        voice != null && !voice.isBlank() ? voice.trim() : catalog.getDefaultVoice();
    String effectiveModel =
        model != null && !model.isBlank() ? model.trim() : catalog.getDefaultModel();
    if (!catalog.containsVoice(effectiveVoice)) {
      throw DomainException.invalid("INVALID_SPEECH_TEXT", "Unknown voice: " + effectiveVoice);
    }
    if (!catalog.containsModel(effectiveModel)) {
      throw DomainException.invalid("INVALID_SPEECH_TEXT", "Unknown model: " + effectiveModel);
    }
    return new VoiceSelection(effectiveVoice, effectiveModel);
  }

  /** Tells whether this selection uses the default voice and model. */
  public boolean isDefault() {
    VoiceCatalog catalog = VoiceCatalog.createDefaultCatalog();
    return catalog.getDefaultVoice().equals(voice) && catalog.getDefaultModel().equals(model);
  }
}
