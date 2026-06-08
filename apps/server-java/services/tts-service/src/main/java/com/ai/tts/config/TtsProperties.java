package com.ai.tts.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TtsProperties {

    @Value("${tts.provider:edge}")
    private String provider;

    @Value("${tts.default-voice:zh-CN-XiaoxiaoNeural}")
    private String defaultVoice;

    @Value("${tts.default-language:zh-CN}")
    private String defaultLanguage;

    @Value("${tts.default-speed:1.0}")
    private float defaultSpeed;

    @Value("${tts.default-pitch:0}")
    private float defaultPitch;

    @Value("${tts.output-dir:${java.io.tmpdir}/tts-output}")
    private String outputDir;

    public String getProvider() {
        return provider;
    }

    public String getDefaultVoice() {
        return defaultVoice;
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public float getDefaultSpeed() {
        return defaultSpeed;
    }

    public float getDefaultPitch() {
        return defaultPitch;
    }

    public String getOutputDir() {
        return outputDir;
    }
}
