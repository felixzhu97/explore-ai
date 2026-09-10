package com.ai.audio.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SpeechStreamingTranscriptionAdapter")
class SpeechStreamingTranscriptionAdapterTest {

  @Test
  @DisplayName("should map http speech base url to websocket uri")
  void shouldMapHttpSpeechBaseUrlToWebsocketUri() {
    assertThat(SpeechStreamingTranscriptionAdapter.toWsUri("http://localhost:8004"))
        .isEqualTo("ws://localhost:8004");
    assertThat(SpeechStreamingTranscriptionAdapter.toWsUri("https://ml.example/"))
        .isEqualTo("wss://ml.example");
  }
}
