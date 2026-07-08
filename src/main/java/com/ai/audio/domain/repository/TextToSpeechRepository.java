package com.ai.audio.domain.repository;

import com.ai.audio.domain.model.SynthesizedAudio;
import com.ai.audio.domain.vo.SpeechText;
import reactor.core.publisher.Flux;

public interface TextToSpeechRepository {

    SynthesizedAudio synthesize(SpeechText text);

    Flux<byte[]> stream(SpeechText text);
}
