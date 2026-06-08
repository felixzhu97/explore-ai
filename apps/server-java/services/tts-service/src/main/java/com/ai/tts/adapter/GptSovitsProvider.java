package com.ai.tts.adapter;

import com.ai.tts.domain.model.OutputFormat;
import com.ai.tts.domain.model.ProviderInfo;
import com.ai.tts.domain.model.Voice;
import com.ai.tts.domain.port.TtsProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public class GptSovitsProvider implements TtsProvider {

    @Override
    public String name() {
        return "gpt-sovits";
    }

    @Override
    public ProviderInfo getInfo() {
        return ProviderInfo.of(
            "gpt-sovits",
            "GPT-SoVITS (Open Source Voice Cloning)",
            List.of("zh-CN", "en-US"),
            List.of(
                "Open-source",
                "Voice cloning from reference audio",
                "Few-shot learning",
                "High quality synthesis"
            )
        );
    }

    @Override
    public Mono<byte[]> synthesize(String text, String voice, String language, float speed, float pitch, OutputFormat format) {
        return Mono.error(new UnsupportedOperationException(
            "GPT-SoVITS provider not yet implemented. Use Edge TTS for now."
        ));
    }

    @Override
    public Flux<byte[]> stream(String text, String voice, String language, float speed, OutputFormat format) {
        return Flux.error(new UnsupportedOperationException(
            "GPT-SoVITS streaming not yet implemented."
        ));
    }

    @Override
    public List<Voice> listVoices(String language) {
        return List.of(
            Voice.of("gpt-sovits-cloned-1", "Cloned Voice 1", "zh-CN", "gpt-sovits"),
            Voice.of("gpt-sovits-cloned-2", "Cloned Voice 2", "en-US", "gpt-sovits")
        );
    }

    @Override
    public boolean healthCheck() {
        return false;
    }
}
