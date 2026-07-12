package com.ai.audio.infrastructure.config;

import com.ai.audio.infrastructure.adapter.AudioTranscriptionWebSocketHandler;
import com.ai.common.config.CorsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/**
 * WebSocket configuration for audio transcription.
 */
@Configuration
@EnableWebSocket
public class AudioWebSocketConfig implements WebSocketConfigurer {

    private final AudioTranscriptionWebSocketHandler transcriptionHandler;
    private final CorsProperties corsProperties;

    public AudioWebSocketConfig(
            AudioTranscriptionWebSocketHandler transcriptionHandler,
            CorsProperties corsProperties) {
        this.transcriptionHandler = transcriptionHandler;
        this.corsProperties = corsProperties;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String[] allowedOriginPatterns = corsProperties.getAllowedOriginPatterns().stream()
                .map(String::trim)
                .toArray(String[]::new);

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
