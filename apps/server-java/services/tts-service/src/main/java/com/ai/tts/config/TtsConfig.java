package com.ai.tts.config;

import com.ai.tts.domain.port.TtsProvider;
import com.ai.tts.adapter.EdgeTtsProvider;
import com.ai.tts.adapter.CosyVoiceProvider;
import com.ai.tts.adapter.GptSovitsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Configuration
public class TtsConfig {

    @Value("${tts.provider:edge}")
    private String providerName;

    @Bean
    public Map<TtsProviderType, TtsProvider> ttsProviders() {
        Map<TtsProviderType, TtsProvider> providers = new EnumMap<>(TtsProviderType.class);
        providers.put(TtsProviderType.EDGE, new EdgeTtsProvider());
        providers.put(TtsProviderType.COSYVOICE, new CosyVoiceProvider());
        providers.put(TtsProviderType.GPT_SOVITS, new GptSovitsProvider());
        return providers;
    }

    @Bean
    public TtsProvider ttsProvider(Map<TtsProviderType, TtsProvider> providers) {
        TtsProviderType type = TtsProviderType.fromString(providerName);
        TtsProvider provider = providers.get(type);
        if (provider == null) {
            provider = providers.get(TtsProviderType.EDGE);
        }
        return provider;
    }

    @Bean
    public List<TtsProvider> allProviders(Map<TtsProviderType, TtsProvider> providers) {
        return List.copyOf(providers.values());
    }

    public enum TtsProviderType {
        EDGE("edge"),
        COSYVOICE("cosyvoice"),
        GPT_SOVITS("gpt-sovits"),
        AZURE("azure"),
        GOOGLE("google"),
        ELEVENLABS("elevenlabs"),
        COQUI("coqui");

        private final String value;

        TtsProviderType(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        public static TtsProviderType fromString(String value) {
            for (TtsProviderType type : values()) {
                if (type.value.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            return EDGE;
        }
    }
}
