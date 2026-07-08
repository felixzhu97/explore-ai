package com.ai.audio.domain.repository;

import com.ai.audio.domain.model.SynthesizedAudio;
import com.ai.audio.domain.vo.SpeechText;

public interface TextToSpeechRepository {

    SynthesizedAudio synthesize(SpeechText text);
}
