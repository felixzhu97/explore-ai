package com.ai.audio.infra.asr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

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

  @Test
  @DisplayName("should send generic error frame when speech upstream cannot be reached")
  void shouldSendGenericErrorFrameWhenSpeechUpstreamCannotBeReached() throws Exception {
    SpeechStreamingTranscriptionClient client =
        new SpeechStreamingTranscriptionClient(
            "http://127.0.0.1:1", Duration.ofSeconds(2), new ObjectMapper());
    WebSocketSession session = mock(WebSocketSession.class);
    when(session.getId()).thenReturn("client-1");
    when(session.isOpen()).thenReturn(true);

    client.streamAudioChunk(session, new StringBuilder(), "{\"type\":\"audio\",\"data\":\"\"}");

    ArgumentCaptor<TextMessage> frame = ArgumentCaptor.forClass(TextMessage.class);
    verify(session).sendMessage(frame.capture());
    assertThat(frame.getValue().getPayload())
        .isEqualTo("{\"type\":\"error\",\"text\":\"Transcription failed\"}");
  }
}
