package com.ai.tts.domain.port;

import com.ai.tts.domain.model.OutputFormat;
import com.ai.tts.domain.model.ProviderInfo;
import com.ai.tts.domain.model.Voice;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface TtsProvider {

    String name();

    ProviderInfo getInfo();

    Mono<byte[]> synthesize(String text, String voice, String language, float speed, float pitch, OutputFormat format);

    Flux<byte[]> stream(String text, String voice, String language, float speed, OutputFormat format);

    List<Voice> listVoices(String language);

    boolean healthCheck();
}
