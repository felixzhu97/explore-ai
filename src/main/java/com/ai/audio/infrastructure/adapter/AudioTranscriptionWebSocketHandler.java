package com.ai.audio.infrastructure.adapter;

import com.ai.audio.application.usecase.StreamingTranscriptionUseCase;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * WebSocket handler for streaming audio transcription.
 */
@Component
public class AudioTranscriptionWebSocketHandler extends TextWebSocketHandler {

    private final StreamingTranscriptionUseCase streamingTranscriptionUseCase;

    public AudioTranscriptionWebSocketHandler(StreamingTranscriptionUseCase streamingTranscriptionUseCase) {
        this.streamingTranscriptionUseCase = streamingTranscriptionUseCase;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        streamingTranscriptionUseCase.startSession(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        streamingTranscriptionUseCase.handleMessage(session, message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        streamingTranscriptionUseCase.endSession(session);
    }
}
