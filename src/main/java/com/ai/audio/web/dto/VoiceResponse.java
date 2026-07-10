package com.ai.audio.web.dto;

public record VoiceResponse(String id, String name, String language, String gender) {

    public static VoiceResponse from(com.ai.audio.domain.vo.VoiceInfo voiceInfo) {
        return new VoiceResponse(
                voiceInfo.id(),
                voiceInfo.name(),
                voiceInfo.language(),
                voiceInfo.gender());
    }
}
