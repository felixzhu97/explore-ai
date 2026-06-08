package com.ai.tts.adapter;

import com.ai.tts.domain.model.OutputFormat;
import com.ai.tts.domain.model.ProviderInfo;
import com.ai.tts.domain.model.Voice;
import com.ai.tts.domain.port.TtsProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public class CosyVoiceProvider implements TtsProvider {

    @Override
    public String name() {
        return "cosyvoice";
    }

    @Override
    public ProviderInfo getInfo() {
        return ProviderInfo.of(
            "cosyvoice",
            "Alibaba CosyVoice (Open Source)",
            List.of("zh-CN", "en-US"),
            List.of(
                "Open-source",
                "Voice cloning support",
                "Multi-language synthesis",
                "Local deployment"
            )
        );
    }

    @Override
    public Mono<byte[]> synthesize(String text, String voice, String language, float speed, float pitch, OutputFormat format) {
        return Mono.error(new UnsupportedOperationException(
            "CosyVoice provider not yet implemented. Use Edge TTS for now."
        ));
    }

    @Override
    public Flux<byte[]> stream(String text, String voice, String language, float speed, OutputFormat format) {
        return Flux.error(new UnsupportedOperationException(
            "CosyVoice streaming not yet implemented."
        ));
    }

    @Override
    public List<Voice> listVoices(String language) {
        return List.of(
            Voice.of("cosyvoice-zh-default", "CosyVoice Chinese Default", "zh-CN", "cosyvoice"),
            Voice.of("cosyvoice-en-default", "CosyVoice English Default", "en-US", "cosyvoice")
        );
    }

    @Override
    public boolean healthCheck() {
        return false;
    }
}
