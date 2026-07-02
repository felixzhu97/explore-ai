package com.ai.audio.infrastructure.config;

import com.ai.audio.infrastructure.adapter.AudioTranscriptionWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket configuration for audio transcription.
 */
@Configuration
@EnableWebSocket
public class AudioWebSocketConfig implements WebSocketConfigurer {

    private final AudioTranscriptionWebSocketHandler transcriptionHandler;

    public AudioWebSocketConfig(AudioTranscriptionWebSocketHandler transcriptionHandler) {
        this.transcriptionHandler = transcriptionHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(transcriptionHandler, "/ws/audio/transcribe");
    }
}
