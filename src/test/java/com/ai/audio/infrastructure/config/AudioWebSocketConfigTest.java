package com.ai.audio.infrastructure.config;

import com.ai.audio.infrastructure.adapter.AudioTranscriptionWebSocketHandler;
import com.ai.common.config.CorsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AudioWebSocketConfig")
class AudioWebSocketConfigTest {

    @Mock
    private AudioTranscriptionWebSocketHandler transcriptionHandler;

    @Mock
    private WebSocketHandlerRegistry registry;

    @Mock
    private WebSocketHandlerRegistration registration;

    private CorsProperties corsProperties;
    private AudioWebSocketConfig audioWebSocketConfig;

    @BeforeEach
    void setUp() {
        corsProperties = new CorsProperties();
        audioWebSocketConfig = new AudioWebSocketConfig(transcriptionHandler, corsProperties);
    }

    @Nested
    @DisplayName("registerWebSocketHandlers")
    class RegisterWebSocketHandlers {

        @Test
        @DisplayName("should allow configured production origins for audio transcription socket")
        void should_allowConfiguredProductionOrigins_when_registeringAudioTranscriptionSocket() {
            corsProperties.setAllowedOriginPatterns(List.of(
                    " https://explore-ai-git-*-felixzhu97s-projects.vercel.app ",
                    "https://www.felixzhu.chat",
                    "https://felixzhu.chat"
            ));
            when(registry.addHandler(transcriptionHandler, "/ws/audio/transcribe"))
                    .thenReturn(registration);

            audioWebSocketConfig.registerWebSocketHandlers(registry);

            verify(registration).setAllowedOriginPatterns(
                    "https://explore-ai-git-*-felixzhu97s-projects.vercel.app",
                    "https://www.felixzhu.chat",
                    "https://felixzhu.chat"
            );
        }

        @Test
        @DisplayName("should register no allowed origin patterns when origins are null")
        void should_registerNoAllowedOriginPatterns_when_originsAreNull() {
            corsProperties.setAllowedOriginPatterns(null);
            when(registry.addHandler(transcriptionHandler, "/ws/audio/transcribe"))
                    .thenReturn(registration);

            audioWebSocketConfig.registerWebSocketHandlers(registry);

            verify(registration).setAllowedOriginPatterns();
        }
    }
}
