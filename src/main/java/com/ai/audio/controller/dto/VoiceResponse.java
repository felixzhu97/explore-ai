package com.ai.audio.controller.dto;

public record VoiceResponse(String id, String name, String language, String gender) {
  /** Maps a catalog voice to the API response. */
  public static VoiceResponse from(com.ai.audio.domain.model.VoiceInfo voiceInfo) {
    return new VoiceResponse(
        voiceInfo.getId(), voiceInfo.getName(), voiceInfo.getLanguage(), voiceInfo.getGender());
  }
}
