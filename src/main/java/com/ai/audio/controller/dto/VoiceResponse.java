package com.ai.audio.controller.dto;

/** Documentation. */
public record VoiceResponse(String id, String name, String language, String gender) {
  /** Documentation. */
  public static VoiceResponse from(com.ai.audio.domain.vo.VoiceInfo voiceInfo) {
    return new VoiceResponse(
        voiceInfo.id(), voiceInfo.name(), voiceInfo.language(), voiceInfo.gender());
  }
}
