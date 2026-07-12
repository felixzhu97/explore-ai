package com.ai.audio.domain.repository;

public interface TtsConfiguration {

    boolean isEnabled();

    boolean isConfigured();

    String getDefaultVoice();
}
