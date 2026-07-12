package com.ai.audio.infrastructure.config;

import com.ai.audio.domain.repository.TtsConfiguration;
import org.springframework.stereotype.Component;

@Component
public class TtsConfigurationAdapter implements TtsConfiguration {

    private final TtsProperties ttsProperties;

    public TtsConfigurationAdapter(TtsProperties ttsProperties) {
        this.ttsProperties = ttsProperties;
    }

    @Override
    public boolean isEnabled() {
        return ttsProperties.isEnabled();
    }

    @Override
    public boolean isConfigured() {
        return ttsProperties.isConfigured();
    }

    @Override
    public String getDefaultVoice() {
        return ttsProperties.getVoice();
    }
}
