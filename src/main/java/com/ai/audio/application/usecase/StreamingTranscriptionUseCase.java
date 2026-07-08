package com.ai.audio.application.usecase;

import com.ai.audio.infrastructure.adapter.WhisperCppTranscriptionAdapter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestrates streaming transcription over WebSocket sessions.
 */
@Service
public class StreamingTranscriptionUseCase {

    private static final Logger log = LoggerFactory.getLogger(StreamingTranscriptionUseCase.class);
    private static final int SEND_TIME_LIMIT_MS = 5_000;
    private static final int BUFFER_SIZE_LIMIT = 512 * 1024;

    private final WhisperCppTranscriptionAdapter whisperCppTranscriptionAdapter;
    private final TaskExecutor transcriptionExecutor;
    private final ObjectMapper objectMapper;

    private final Map<String, SessionState> sessions = new ConcurrentHashMap<>();

    public StreamingTranscriptionUseCase(
            WhisperCppTranscriptionAdapter whisperCppTranscriptionAdapter,
            @Qualifier("asrTranscriptionExecutor") TaskExecutor transcriptionExecutor,
            ObjectMapper objectMapper) {
        this.whisperCppTranscriptionAdapter = whisperCppTranscriptionAdapter;
        this.transcriptionExecutor = transcriptionExecutor;
        this.objectMapper = objectMapper;
    }

    /**
     * Start a new transcription session.
     */
    public void startSession(WebSocketSession rawSession) {
        WebSocketSession session = new ConcurrentWebSocketSessionDecorator(
                rawSession, SEND_TIME_LIMIT_MS, BUFFER_SIZE_LIMIT);
        sessions.put(rawSession.getId(), new SessionState(UUID.randomUUID().toString(), session));
        log.info("Started transcription session: {}", rawSession.getId());
    }

    /**
     * Handle incoming WebSocket message from client.
     */
    public void handleMessage(WebSocketSession rawSession, String payload) {
        SessionState state = sessions.get(rawSession.getId());
        if (state == null) {
            log.warn("Received message for unknown session: {}", rawSession.getId());
            return;
        }

        String messageType = extractMessageType(payload);
        switch (messageType) {
            case "audio" -> transcriptionExecutor.execute(() -> processAudioChunk(rawSession.getId(), state, payload));
            case "stop" -> transcriptionExecutor.execute(() -> finalizeAndClose(rawSession.getId(), state));
            default -> whisperCppTranscriptionAdapter.sendError(
                    state.session, "Unsupported message type: " + messageType);
        }
    }

    /**
     * Clean up session resources after connection is closed.
     */
    public void endSession(WebSocketSession rawSession) {
        sessions.remove(rawSession.getId());
        log.info("Ended transcription session: {}", rawSession.getId());
    }

    private void processAudioChunk(String sessionId, SessionState state, String payload) {
        synchronized (state) {
            if (!sessions.containsKey(sessionId)) {
                return;
            }
            whisperCppTranscriptionAdapter.streamAudioChunk(state.session, state.transcript, payload);
        }
    }

    private void finalizeAndClose(String sessionId, SessionState state) {
        synchronized (state) {
            if (sessions.remove(sessionId) == null) {
                return;
            }
            whisperCppTranscriptionAdapter.finalizeSession(state.session, state.transcript);
            closeQuietly(state.session);
        }
    }

    private String extractMessageType(String payload) {
        try {
            Map<String, String> message = objectMapper.readValue(payload, new TypeReference<>() {});
            return message.getOrDefault("type", "");
        } catch (Exception e) {
            log.warn("Failed to parse WebSocket message type", e);
            return "";
        }
    }

    private void closeQuietly(WebSocketSession session) {
        if (!session.isOpen()) {
            return;
        }
        try {
            session.close(CloseStatus.NORMAL);
        } catch (Exception e) {
            log.warn("Failed to close WebSocket session {}", session.getId(), e);
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
