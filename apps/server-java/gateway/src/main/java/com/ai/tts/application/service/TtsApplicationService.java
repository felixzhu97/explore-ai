package com.ai.tts.application.service;

import com.ai.tts.domain.AudioResult;
import com.ai.tts.domain.OutputFormat;
import com.ai.tts.domain.ProviderInfo;
import com.ai.tts.domain.Speech;
import com.ai.tts.domain.SynthesisRequest;
import com.ai.tts.domain.TtsProvider;
import com.ai.tts.domain.Voice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TtsApplicationService {

    private static final Logger log = LoggerFactory.getLogger(TtsApplicationService.class);

    private final Map<String, TtsProvider> providers;
    private final TtsProvider defaultProvider;

    public TtsApplicationService(List<TtsProvider> ttsProviders, TtsProvider defaultTtsProvider) {
        this.providers = ttsProviders.stream()
                .collect(Collectors.toMap(TtsProvider::name, Function.identity()));
        this.defaultProvider = defaultTtsProvider;
        log.info("Initialized TtsApplicationService with {} providers: {}",
                providers.size(), providers.keySet());
    }

    public Mono<AudioResult> synthesize(String text, String voice, String language,
                                        float speed, float pitch, OutputFormat format, String providerName) {
        log.debug("Synthesize request: text={}, voice={}, language={}, speed={}, pitch={}, format={}, provider={}",
                text, voice, language, speed, pitch, format, providerName);

        Speech speech = Speech.fromRequest(text, voice, language, speed, pitch, format, providerName);
        TtsProvider provider = resolveProvider(speech.provider());

        speech.startSynthesis();

        return provider.synthesize(
                speech.text(),
                speech.voice(),
                speech.language(),
                speech.speed(),
                speech.pitch(),
                speech.outputFormat()
            )
            .doOnNext(audio -> speech.complete(audio))
            .doOnError(e -> speech.fail(e))
            .map(audio -> AudioResult.of(speech.id(), audio, speech.outputFormat()))
            .doOnSuccess(result -> log.info("Synthesis completed: speechId={}, size={} bytes",
                    result.speechId().value(), result.sizeInBytes()))
            .doOnError(e -> log.error("Synthesis failed: {}", e.getMessage(), e));
    }

    public Flux<byte[]> stream(String text, String voice, String language,
                               float speed, OutputFormat format, String providerName) {
        log.debug("Stream request: text={}, voice={}, language={}, speed={}, format={}, provider={}",
                text, voice, language, speed, format, providerName);

        Speech speech = Speech.fromRequest(text, voice, language, speed, 0, format, providerName);
        TtsProvider provider = resolveProvider(speech.provider());

        return provider.stream(
                speech.text(),
                speech.voice(),
                speech.language(),
                speech.speed(),
                speech.outputFormat()
            )
            .doOnNext(audio -> log.debug("Streaming chunk: {} bytes", audio.length))
            .doOnError(e -> log.error("Streaming failed: {}", e.getMessage(), e));
    }

    public List<Voice> listVoices(String language, String providerName) {
        TtsProvider provider = resolveProvider(providerName);
        return provider.listVoices(language);
    }

    public List<ProviderInfo> listAllProviders() {
        String defaultName = defaultProvider.name();
        return providers.values().stream()
                .map(p -> p.getInfo().name().equals(defaultName)
                    ? ProviderInfo.active(p.getInfo().name(), p.getInfo().displayName(),
                        p.getInfo().supportedLanguages(), p.getInfo().features())
                    : p.getInfo())
                .toList();
    }

    public ProviderInfo getCurrentProviderInfo(String providerName) {
        TtsProvider provider = resolveProvider(providerName);
        ProviderInfo info = provider.getInfo();
        return ProviderInfo.active(info.name(), info.displayName(), info.supportedLanguages(), info.features());
    }

    public boolean healthCheck(String providerName) {
        TtsProvider provider = resolveProvider(providerName);
        return provider.healthCheck();
    }

    public TtsProvider resolveProvider(String providerName) {
        if (providerName != null && !providerName.isBlank()) {
            String key = providerName.toLowerCase();
            TtsProvider provider = providers.get(key);
            if (provider != null) {
                return provider;
            }
        }
        log.debug("Using default provider: {}", defaultProvider.name());
        return defaultProvider;
    }

    public TtsProvider getDefaultProvider() {
        return defaultProvider;
    }
}
