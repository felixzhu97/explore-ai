package com.ai.audio.web;

public record VoiceResponse(String id, String name, String language, String gender) {

    public static VoiceResponse from(com.ai.audio.domain.VoiceInfo voiceInfo) {
        return new VoiceResponse(
                voiceInfo.id(),
                voiceInfo.name(),
                voiceInfo.language(),
                voiceInfo.gender());
    }
}
