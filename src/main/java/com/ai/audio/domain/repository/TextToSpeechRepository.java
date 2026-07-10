package com.ai.audio.domain.repository;

import com.ai.audio.domain.model.SynthesizedAudio;
import com.ai.audio.domain.vo.SpeechText;
import com.ai.audio.domain.vo.VoiceSelection;

public interface TextToSpeechRepository {

    SynthesizedAudio synthesize(SpeechText text, VoiceSelection voiceSelection, Double speed);
}
