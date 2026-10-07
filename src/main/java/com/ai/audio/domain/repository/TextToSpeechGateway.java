package com.ai.audio.domain.repository;

import com.ai.audio.domain.model.SpeechText;
import com.ai.audio.domain.model.SynthesizedAudio;
import com.ai.audio.domain.model.VoiceSelection;

/** Repository that synthesizes speech audio for text with a chosen voice and speed. */
public interface TextToSpeechGateway {
  /** Synthesizes speech for the text with the chosen voice and speed. */
  SynthesizedAudio synthesizeSpeech(SpeechText text, VoiceSelection voiceSelection, Double speed);
}
