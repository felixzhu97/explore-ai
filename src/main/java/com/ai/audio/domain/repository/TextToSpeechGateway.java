package com.ai.audio.domain.repository;

import com.ai.audio.domain.model.SynthesizedAudio;
import com.ai.audio.domain.vo.SpeechText;
import com.ai.audio.domain.vo.VoiceSelection;

/** Repository that synthesizes speech audio for text with a chosen voice and speed. */
public interface TextToSpeechGateway {
  SynthesizedAudio synthesize(SpeechText text, VoiceSelection voiceSelection, Double speed);
}
