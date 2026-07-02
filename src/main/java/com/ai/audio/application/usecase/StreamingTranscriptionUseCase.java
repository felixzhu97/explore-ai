package com.ai.audio.application.usecase;

import com.ai.audio.infrastructure.adapter.OllamaWhisperAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestrates streaming transcription over WebSocket sessions.
 */
@Service
public class StreamingTranscriptionUseCase {

    private static final Logger log = LoggerFactory.getLogger(StreamingTranscriptionUseCase.class);

    private final OllamaWhisperAdapter ollamaWhisperAdapter;

    private final Map<WebSocketSession, SessionState> sessions = new ConcurrentHashMap<>();

    public StreamingTranscriptionUseCase(OllamaWhisperAdapter ollamaWhisperAdapter) {
        this.ollamaWhisperAdapter = ollamaWhisperAdapter;
    }

    /**
     * Start a new transcription session.
     */
    public void startSession(WebSocketSession session) {
        sessions.put(session, new SessionState(UUID.randomUUID().toString()));
        log.info("Started transcription session: {}", session.getId());
    }

    /**
     * Handle incoming audio chunk from client.
     */
    public void handleAudioChunk(WebSocketSession session, String payload) {
        SessionState state = sessions.get(session);
        if (state == null) {
            log.warn("Received audio chunk for unknown session: {}", session.getId());
            return;
        }

        ollamaWhisperAdapter.streamAudioChunk(session, state.transcript, payload);
    }

    /**
     * End transcription session and clean up resources.
     */
    public void endSession(WebSocketSession session) {
        SessionState state = sessions.remove(session);
        if (state != null) {
            ollamaWhisperAdapter.finalizeSession(session, state.transcript);
            log.info("Ended transcription session: {}", session.getId());
        }
    }

    static class SessionState {
        final String sessionId;
        final StringBuilder transcript;

        SessionState(String sessionId) {
            this.sessionId = sessionId;
            this.transcript = new StringBuilder();
        }
    }
}
