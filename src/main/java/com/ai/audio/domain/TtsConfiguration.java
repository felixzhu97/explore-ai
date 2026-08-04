package com.ai.audio.domain;

public interface TtsConfiguration {

    boolean isEnabled();

    boolean isConfigured();

    String getDefaultVoice();
}
