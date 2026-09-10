package com.ai.audio.infra.adapter;

import com.ai.audio.domain.repository.StreamingTranscriptionGateway;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Proxies product {@code /ws/audio/transcribe} frames to explore-ml media-gen {@code
 * /ws/v1/audios:transcribe} (Qwen3-ASR).
 */
@Component
public class MediaGenStreamingTranscriptionAdapter implements StreamingTranscriptionGateway {

  private static final Logger log =
      LoggerFactory.getLogger(MediaGenStreamingTranscriptionAdapter.class);

  private final String wsUri;
  private final Duration connectTimeout;
  private final ObjectMapper objectMapper;
  private final Map<String, WebSocketSession> upstreamByClient = new ConcurrentHashMap<>();

  /** Documentation. */
  public MediaGenStreamingTranscriptionAdapter(
      @Value("${app.asr.media-gen.base-url:${MEDIA_GENERATION_API_URL:http://localhost:8003}}")
          String baseUrl,
      @Value("${app.asr.media-gen.connect-timeout:5s}") Duration connectTimeout,
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
        sendError(session, "media-gen ASR upstream unavailable");
        return;
      }
      upstream.sendMessage(new TextMessage(payload));
    } catch (Exception e) {
      log.error("Failed to forward audio chunk to media-gen", e);
      sendError(session, "Transcription failed: " + e.getMessage());
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
    sendMessage(session, "error", text);
  }

  private void forwardControl(
      WebSocketSession session, StringBuilder transcript, String controlJson) {
    try {
      WebSocketSession upstream = ensureUpstream(session, transcript);
      if (upstream != null && upstream.isOpen()) {
        upstream.sendMessage(new TextMessage(controlJson));
      }
    } catch (Exception e) {
      log.warn("Failed to forward control frame to media-gen", e);
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
      Map<String, String> message = objectMapper.readValue(payload, new TypeReference<>() {});
      String type = message.getOrDefault("type", "");
      String text = message.getOrDefault("text", "");
      if ("partial".equals(type) || "final".equals(type)) {
        if (text != null && !text.isBlank()) {
          synchronized (transcript) {
            transcript.setLength(0);
            transcript.append(text);
          }
        }
      }
      if (client.isOpen()) {
        client.sendMessage(new TextMessage(payload));
      }
    } catch (Exception e) {
      log.warn("Failed to relay media-gen ASR frame", e);
      sendError(client, "Transcription relay failed");
    }
  }

  private void closeUpstream(String clientSessionId) {
    WebSocketSession upstream = upstreamByClient.remove(clientSessionId);
    if (upstream != null && upstream.isOpen()) {
      try {
        upstream.close(CloseStatus.NORMAL);
      } catch (Exception e) {
        log.debug("Upstream close failed: {}", e.toString());
      }
    }
  }

  private void sendMessage(WebSocketSession session, String type, String text) {
    if (!session.isOpen()) {
      return;
    }
    try {
      String json = objectMapper.writeValueAsString(Map.of("type", type, "text", text));
      session.sendMessage(new TextMessage(json));
    } catch (Exception e) {
      log.error("Error sending WebSocket message", e);
    }
  }

  static String toWsUri(String httpBase) {
    String base = httpBase == null ? "http://localhost:8003" : httpBase.trim();
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
