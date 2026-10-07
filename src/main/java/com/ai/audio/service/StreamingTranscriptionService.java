package com.ai.audio.service;

import com.ai.audio.domain.repository.StreamingTranscriptionGateway;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

/** Orchestrates streaming transcription over WebSocket sessions. */
@Service
public class StreamingTranscriptionService {

  private static final int SEND_TIME_LIMIT_MS = 5_000;
  private static final int BUFFER_SIZE_LIMIT = 512 * 1024;

  private final StreamingTranscriptionGateway transcriptionGateway;
  private final TaskExecutor transcriptionExecutor;
  private final ObjectMapper objectMapper;

  private final Map<String, SessionState> sessions = new ConcurrentHashMap<>();

  public StreamingTranscriptionService(
      StreamingTranscriptionGateway transcriptionGateway,
      @Qualifier("asrTranscriptionExecutor") TaskExecutor transcriptionExecutor,
      ObjectMapper objectMapper) {
    this.transcriptionGateway = transcriptionGateway;
    this.transcriptionExecutor = transcriptionExecutor;
    this.objectMapper = objectMapper;
  }

  /** Start a new transcription session. */
  public void startSession(WebSocketSession socket) {
    WebSocketSession session =
        new ConcurrentWebSocketSessionDecorator(socket, SEND_TIME_LIMIT_MS, BUFFER_SIZE_LIMIT);
    sessions.put(socket.getId(), new SessionState(UUID.randomUUID().toString(), session));
  }

  /** Handle incoming WebSocket message from client. */
  public void handleMessage(WebSocketSession socket, String message) {
    SessionState state = sessions.get(socket.getId());
    if (state == null) {
      return;
    }

    String messageType = extractMessageType(message);
    switch (messageType) {
      case "audio" ->
          transcriptionExecutor.execute(() -> processAudioChunk(socket.getId(), state, message));
      case "commit" -> transcriptionExecutor.execute(() -> commitTurn(socket.getId(), state));
      case "stop" -> transcriptionExecutor.execute(() -> closeSession(socket.getId(), state));
      default ->
          transcriptionGateway.sendError(state.session, "Unsupported message type: " + messageType);
    }
  }

  /** Clean up session resources after connection is closed. */
  public void endSession(WebSocketSession socket) {
    sessions.remove(socket.getId());
  }

  private void processAudioChunk(String sessionId, SessionState state, String message) {
    synchronized (state) {
      if (!sessions.containsKey(sessionId)) {
        return;
      }
      transcriptionGateway.streamAudioChunk(state.session, state.transcript, message);
    }
  }

  private void commitTurn(String sessionId, SessionState state) {
    synchronized (state) {
      if (!sessions.containsKey(sessionId)) {
        return;
      }
      transcriptionGateway.commitTurn(state.session, state.transcript);
    }
  }

  private void closeSession(String sessionId, SessionState state) {
    synchronized (state) {
      if (sessions.remove(sessionId) == null) {
        return;
      }
      transcriptionGateway.finalizeSession(state.session, state.transcript);
      closeQuietly(state.session);
    }
  }

  private String extractMessageType(String message) {
    try {
      Map<String, String> fields = objectMapper.readValue(message, new TypeReference<>() {});
      return fields.getOrDefault("type", "");
    } catch (Exception e) {
      return "";
    }
  }

  private void closeQuietly(WebSocketSession session) {
    if (!session.isOpen()) {
      return;
    }
    try {
      session.close(CloseStatus.NORMAL);
    } catch (Exception expected) {
    }
  }

  static class SessionState {
    final String sessionId;
    final WebSocketSession session;
    final StringBuilder transcript;

    SessionState(String sessionId, WebSocketSession session) {
      this.sessionId = sessionId;
      this.session = session;
      this.transcript = new StringBuilder();
    }
  }
}
