package com.ai.audio.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SynthesizedAudio")
class SynthesizedAudioTest {

    @Test
    @DisplayName("should expose size and empty state")
    void should_expose_size_and_empty_state() {
        SynthesizedAudio audio = SynthesizedAudio.create("data".getBytes());

        assertThat(audio.isEmpty()).isFalse();
        assertThat(audio.sizeInBytes()).isEqualTo(4);
    }

    @Test
    @DisplayName("should return defensive copy of bytes")
    void should_return_defensive_copy_of_bytes() {
        byte[] raw = "data".getBytes();
        SynthesizedAudio audio = SynthesizedAudio.create(raw);
        raw[0] = 'X';

        assertThat(audio.data()[0]).isEqualTo((byte) 'd');
    }
}
