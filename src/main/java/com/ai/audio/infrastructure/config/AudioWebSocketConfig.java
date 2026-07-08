package com.ai.audio.infrastructure.config;

import com.ai.audio.infrastructure.adapter.AudioTranscriptionWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import java.util.Arrays;

/**
 * WebSocket configuration for audio transcription.
 */
@Configuration
@EnableWebSocket
public class AudioWebSocketConfig implements WebSocketConfigurer {

    private final AudioTranscriptionWebSocketHandler transcriptionHandler;
    private final String[] allowedOriginPatterns;

    public AudioWebSocketConfig(
            AudioTranscriptionWebSocketHandler transcriptionHandler,
            @Value("${app.cors.allowed-origin-patterns:http://localhost:4200,http://localhost:3000}") String[] allowedOriginPatterns) {
        this.transcriptionHandler = transcriptionHandler;
        this.allowedOriginPatterns = Arrays.stream(allowedOriginPatterns)
                .map(String::trim)
                .toArray(String[]::new);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(transcriptionHandler, "/ws/audio/transcribe")
                .setAllowedOriginPatterns(allowedOriginPatterns);
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        var container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(512 * 1024);
        container.setMaxBinaryMessageBufferSize(512 * 1024);
        return container;
    }
}
