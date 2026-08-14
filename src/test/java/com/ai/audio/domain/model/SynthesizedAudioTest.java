package com.ai.audio.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SynthesizedAudio")
class SynthesizedAudioTest {

  @Test
  @DisplayName("should expose size and empty state")
  void shouldExposeSizeAndEmptyState() {
    SynthesizedAudio audio = SynthesizedAudio.create("data".getBytes());

    assertThat(audio.isEmpty()).isFalse();
    assertThat(audio.sizeInBytes()).isEqualTo(4);
  }

  @Test
  @DisplayName("should return defensive copy of bytes")
  void shouldReturnDefensiveCopyOfBytes() {
    byte[] raw = "data".getBytes();
    SynthesizedAudio audio = SynthesizedAudio.create(raw);
    raw[0] = 'X';

    assertThat(audio.data()[0]).isEqualTo((byte) 'd');
  }
}
