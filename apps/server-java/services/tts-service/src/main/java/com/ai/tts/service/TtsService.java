package com.ai.tts.service;

import com.ai.tts.domain.model.OutputFormat;
import com.ai.tts.domain.model.ProviderInfo;
import com.ai.tts.domain.model.Voice;
import com.ai.tts.domain.port.TtsProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public class TtsService {

    private final Map<String, TtsProvider> providers;
    private final TtsProvider defaultProvider;

    public TtsService(List<TtsProvider> providers, TtsProvider defaultProvider) {
        this.providers = providers.stream()
                .collect(java.util.stream.Collectors.toMap(TtsProvider::name, p -> p));
        this.defaultProvider = defaultProvider;
    }

    public Mono<byte[]> synthesize(String text, String voice, String language, 
                                   float speed, float pitch, OutputFormat format, String providerName) {
        TtsProvider provider = resolveProvider(providerName);
        return provider.synthesize(text, voice, language, speed, pitch, format);
    }

    public Flux<byte[]> stream(String text, String voice, String language,
                               float speed, OutputFormat format, String providerName) {
        TtsProvider provider = resolveProvider(providerName);
        return provider.stream(text, voice, language, speed, format);
    }

    public List<Voice> listVoices(String language, String providerName) {
        TtsProvider provider = resolveProvider(providerName);
        return provider.listVoices(language);
    }

    public List<ProviderInfo> listAllProviders() {
        return providers.values().stream()
                .map(TtsProvider::getInfo)
                .toList();
    }

    public ProviderInfo getCurrentProviderInfo(String providerName) {
        TtsProvider provider = resolveProvider(providerName);
        return provider.getInfo();
    }

    public boolean healthCheck(String providerName) {
        TtsProvider provider = resolveProvider(providerName);
        return provider.healthCheck();
    }

    public TtsProvider resolveProvider(String providerName) {
        if (providerName != null && !providerName.isBlank()) {
            TtsProvider provider = providers.get(providerName.toLowerCase());
            if (provider != null) {
                return provider;
            }
        }
        return defaultProvider;
    }
}
