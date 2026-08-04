package com.ai.audio.domain;

import com.ai.audio.domain.SynthesizedAudio;
import com.ai.audio.domain.SpeechText;
import com.ai.audio.domain.VoiceSelection;

public interface TextToSpeechRepository {

    SynthesizedAudio synthesize(SpeechText text, VoiceSelection voiceSelection, Double speed);
}
