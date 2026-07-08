package com.ai.audio.application.usecase;

import com.ai.audio.infrastructure.adapter.WhisperCppTranscriptionAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StreamingTranscriptionUseCase Tests")
class StreamingTranscriptionUseCaseTest {

    @Mock
    private WhisperCppTranscriptionAdapter whisperCppTranscriptionAdapter;

    @Mock
    private WebSocketSession rawSession;

    @Mock
    private WebSocketSession decoratedSession;

    private StreamingTranscriptionUseCase useCase;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        useCase = new StreamingTranscriptionUseCase(
                whisperCppTranscriptionAdapter, new SyncTaskExecutor(), objectMapper);
        when(rawSession.getId()).thenReturn("session-1");
    }

    @Test
    @DisplayName("should finalize and close when stop message received")
    void should_finalizeAndClose_when_stopMessageReceived() throws Exception {
        when(rawSession.isOpen()).thenReturn(true);
        useCase.startSession(rawSession);

        String payload = objectMapper.writeValueAsString(java.util.Map.of("type", "stop"));
        useCase.handleMessage(rawSession, payload);

        verify(whisperCppTranscriptionAdapter).finalizeSession(any(WebSocketSession.class), any(StringBuilder.class));
        verify(rawSession).close(CloseStatus.NORMAL);
    }

    @Test
    @DisplayName("should send error when message type is unsupported")
    void should_sendError_when_messageTypeIsUnsupported() throws Exception {
        useCase.startSession(rawSession);

        String payload = objectMapper.writeValueAsString(java.util.Map.of("type", "ping"));
        useCase.handleMessage(rawSession, payload);

        verify(whisperCppTranscriptionAdapter).sendError(any(WebSocketSession.class), eq("Unsupported message type: ping"));
    }

    @Test
    @DisplayName("should process audio chunk asynchronously")
    void should_processAudioChunk_when_audioMessageReceived() throws Exception {
        useCase.startSession(rawSession);

        String payload = objectMapper.writeValueAsString(
                java.util.Map.of("type", "audio", "data", "d2F2"));
        useCase.handleMessage(rawSession, payload);

        verify(whisperCppTranscriptionAdapter).streamAudioChunk(
                any(WebSocketSession.class), any(StringBuilder.class), eq(payload));
    }

    @Test
    @DisplayName("should cleanup session without sending final on abrupt disconnect")
    void should_cleanupOnly_when_connectionClosedWithoutStop() {
        useCase.startSession(rawSession);

        useCase.endSession(rawSession);

        verify(whisperCppTranscriptionAdapter, never()).finalizeSession(any(), any());
    }
}
