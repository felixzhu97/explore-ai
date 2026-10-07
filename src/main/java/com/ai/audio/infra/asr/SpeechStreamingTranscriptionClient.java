package com.ai.audio.infra.asr;

import com.ai.audio.controller.dto.TranscriptionResponse;
import com.ai.audio.controller.dto.TranscriptionResponse.TranscriptionType;
import com.ai.audio.domain.repository.StreamingTranscriptionGateway;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Proxies product {@code /ws/audio/transcribe} frames to explore-ml speech {@code
 * /ws/v1/audios:transcribe} (Qwen3-ASR).
 */
@Component
public class SpeechStreamingTranscriptionClient implements StreamingTranscriptionGateway {

  /** Client-facing reason; exception details are never sent to the client. */
  static final String TRANSCRIPTION_FAILED = "Transcription failed";

  private final String wsUri;
  private final Duration connectTimeout;
  private final ObjectMapper objectMapper;
  private final Map<String, WebSocketSession> upstreamByClient = new ConcurrentHashMap<>();

  public SpeechStreamingTranscriptionClient(
      @Value("${app.asr.speech.base-url:${EXPLORE_ML_API_URL:http://localhost:8000}}")
          String baseUrl,
      @Value("${app.asr.speech.connect-timeout:5s}") Duration connectTimeout,
      ObjectMapper objectMapper) {
    this.wsUri = toWsUri(baseUrl) + "/ws/v1/audios:transcribe";
    this.connectTimeout = connectTimeout;
    this.objectMapper = objectMapper;
  }

  @Override
  public void streamAudioChunk(WebSocketSession session, StringBuilder transcript, String payload) {
    try {
      WebSocketSession upstream = ensureUpstream(session, transcript);
      if (upstream == null || !upstream.isOpen()) {
        sendError(session, "speech ASR upstream unavailable");
        return;
      }
      upstream.sendMessage(new TextMessage(payload));
    } catch (Exception e) {
      sendError(session, TRANSCRIPTION_FAILED);
    }
  }

  @Override
  public void commitTurn(WebSocketSession session, StringBuilder transcript) {
    forwardControl(session, transcript, "{\"type\":\"commit\"}");
  }

  @Override
  public void finalizeSession(WebSocketSession session, StringBuilder transcript) {
    forwardControl(session, transcript, "{\"type\":\"stop\"}");
    closeUpstream(session.getId());
  }

  @Override
  public void sendError(WebSocketSession session, String text) {
    sendFrame(session, TranscriptionResponse.createErrorResponse(text));
  }

  private void forwardControl(
      WebSocketSession session, StringBuilder transcript, String controlJson) {
    try {
      WebSocketSession upstream = ensureUpstream(session, transcript);
      if (upstream != null && upstream.isOpen()) {
        upstream.sendMessage(new TextMessage(controlJson));
      }
    } catch (Exception expected) {
    }
  }

  private WebSocketSession ensureUpstream(WebSocketSession client, StringBuilder transcript)
      throws Exception {
    WebSocketSession existing = upstreamByClient.get(client.getId());
    if (existing != null && existing.isOpen()) {
      return existing;
    }
    StandardWebSocketClient wsClient = new StandardWebSocketClient();
    WebSocketSession upstream =
        wsClient
            .execute(
                new TextWebSocketHandler() {
                  @Override
                  protected void handleTextMessage(
                      WebSocketSession upstreamSession, TextMessage message) {
                    relayUpstream(client, transcript, message.getPayload());
                  }

                  @Override
                  public void afterConnectionClosed(
                      WebSocketSession upstreamSession, CloseStatus status) {
                    upstreamByClient.remove(client.getId(), upstreamSession);
                  }
                },
                wsUri)
            .get(connectTimeout.toMillis(), TimeUnit.MILLISECONDS);
    upstreamByClient.put(client.getId(), upstream);
    return upstream;
  }

  private void relayUpstream(WebSocketSession client, StringBuilder transcript, String payload) {
    try {
      TranscriptionResponse frame = objectMapper.readValue(payload, TranscriptionResponse.class);
      if (frame.type() != TranscriptionType.ERROR && !frame.text().isBlank()) {
        synchronized (transcript) {
          transcript.setLength(0);
          transcript.append(frame.text());
        }
      }
      sendFrame(client, frame);
    } catch (Exception e) {
      sendError(client, "Transcription relay failed");
    }
  }

  private void closeUpstream(String clientSessionId) {
    WebSocketSession upstream = upstreamByClient.remove(clientSessionId);
    if (upstream != null && upstream.isOpen()) {
      try {
        upstream.close(CloseStatus.NORMAL);
      } catch (Exception expected) {
      }
    }
  }

  private void sendFrame(WebSocketSession session, TranscriptionResponse frame) {
    if (!session.isOpen()) {
      return;
    }
    try {
      session.sendMessage(new TextMessage(objectMapper.writeValueAsString(frame)));
    } catch (Exception expected) {
    }
  }

  /** Converts the speech service HTTP base URL to its WebSocket URI. */
  static String toWsUri(String httpBase) {
    String base = httpBase == null ? "http://localhost:8000" : httpBase.trim();
    while (base.endsWith("/")) {
      base = base.substring(0, base.length() - 1);
    }
    if (base.startsWith("https://")) {
      return "wss://" + base.substring("https://".length());
    }
    if (base.startsWith("http://")) {
      return "ws://" + base.substring("http://".length());
    }
    if (base.startsWith("ws://") || base.startsWith("wss://")) {
      return base;
    }
    return "ws://" + base;
  }
}
