package com.ai.audio.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MediaGenStreamingTranscriptionAdapter")
class MediaGenStreamingTranscriptionAdapterTest {

  @Test
  @DisplayName("should map http media-gen base url to websocket uri")
  void shouldMapHttpMediaGenBaseUrlToWebsocketUri() {
    assertThat(MediaGenStreamingTranscriptionAdapter.toWsUri("http://localhost:8003"))
        .isEqualTo("ws://localhost:8003");
    assertThat(MediaGenStreamingTranscriptionAdapter.toWsUri("https://ml.example/"))
        .isEqualTo("wss://ml.example");
  }
}
