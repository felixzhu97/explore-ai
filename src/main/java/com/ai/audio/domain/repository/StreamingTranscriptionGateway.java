package com.ai.audio.domain.repository;

import org.springframework.web.socket.WebSocketSession;

/**
 * Outbound gateway for streaming speech recognition. Implementations call whisper.cpp or explore-ml
 * media-gen.
 */
public interface StreamingTranscriptionGateway {

  /** Transcribe one client audio chunk and emit partial results on the session. */
  void streamAudioChunk(WebSocketSession session, StringBuilder transcript, String payload);

  /** Emit final transcript for the session. */
  void finalizeSession(WebSocketSession session, StringBuilder transcript);

  /** Send a protocol error frame to the client. */
  void sendError(WebSocketSession session, String text);

  /** Optional: forward an explicit commit turn (media-gen). Default no-op. */
  default void commitTurn(WebSocketSession session, StringBuilder transcript) {
    finalizeSession(session, transcript);
  }
}
