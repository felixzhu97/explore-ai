package com.ai.audio.infra.websocket;

import com.ai.audio.service.StreamingTranscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/** WebSocket handler for streaming audio transcription. */
@Component
@RequiredArgsConstructor
public class AudioTranscriptionWebSocketHandler extends TextWebSocketHandler {

  private final StreamingTranscriptionService streamingTranscriptionService;

  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    streamingTranscriptionService.startSession(session);
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) {
    streamingTranscriptionService.handleMessage(session, message.getPayload());
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    streamingTranscriptionService.endSession(session);
  }
}
