package com.ai.audio.infra.asr;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SpeechStreamingTranscriptionClient")
class SpeechStreamingTranscriptionClientTest {

  @Test
  @DisplayName("should map http speech base url to websocket uri")
  void shouldMapHttpSpeechBaseUrlToWebsocketUri() {
    assertThat(SpeechStreamingTranscriptionClient.toWsUri("http://localhost:8000"))
        .isEqualTo("ws://localhost:8000");
    assertThat(SpeechStreamingTranscriptionClient.toWsUri("https://ml.example/"))
        .isEqualTo("wss://ml.example");
  }
}
