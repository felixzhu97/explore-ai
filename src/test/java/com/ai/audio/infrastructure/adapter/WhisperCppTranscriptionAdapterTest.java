package com.ai.audio.infrastructure.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
@DisplayName("WhisperCppTranscriptionAdapter Tests")
class WhisperCppTranscriptionAdapterTest {

    @Mock
    private WebSocketSession session;

    private MockRestServiceServer mockServer;
    private WhisperCppTranscriptionAdapter adapter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8178");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        adapter = new WhisperCppTranscriptionAdapter(builder.build(), "whisper-base", objectMapper);
    }

    @Test
    @DisplayName("should send partial when transcription succeeds")
    void should_sendPartial_when_transcriptionSucceeds() throws Exception {
        when(session.isOpen()).thenReturn(true);
        mockServer.expect(requestTo("http://localhost:8178/v1/audio/transcriptions"))
                .andExpect(method(POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.MULTIPART_FORM_DATA))
                .andRespond(withSuccess("{\"text\":\"hello world\"}", MediaType.APPLICATION_JSON));

        String payload = objectMapper.writeValueAsString(
                java.util.Map.of("type", "audio", "data", java.util.Base64.getEncoder().encodeToString("wav".getBytes())));
        StringBuilder transcript = new StringBuilder();

        adapter.streamAudioChunk(session, transcript, payload);

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        assertThat(captor.getValue().getPayload()).contains("\"type\":\"partial\"");
        assertThat(captor.getValue().getPayload()).contains("hello world");
        assertThat(transcript).hasToString("hello world");
        mockServer.verify();
    }

    @Test
    @DisplayName("should send error when whisper server returns non-200")
    void should_sendError_when_whisperServerReturnsNon200() throws Exception {
        when(session.isOpen()).thenReturn(true);
        mockServer.expect(requestTo("http://localhost:8178/v1/audio/transcriptions"))
                .andExpect(method(POST))
                .andRespond(withBadRequest().body("{\"error\":\"model not found\"}"));

        String payload = objectMapper.writeValueAsString(
                java.util.Map.of("type", "audio", "data", java.util.Base64.getEncoder().encodeToString("wav".getBytes())));
        StringBuilder transcript = new StringBuilder();

        adapter.streamAudioChunk(session, transcript, payload);

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        assertThat(captor.getValue().getPayload()).contains("\"type\":\"error\"");
        assertThat(transcript).isEmpty();
        mockServer.verify();
    }

    @Test
    @DisplayName("should send error when message type is not audio")
    void should_sendError_when_messageTypeIsNotAudio() throws Exception {
        when(session.isOpen()).thenReturn(true);
        String payload = objectMapper.writeValueAsString(
                java.util.Map.of("type", "stop", "data", "ignored"));
        StringBuilder transcript = new StringBuilder();

        adapter.streamAudioChunk(session, transcript, payload);

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        assertThat(captor.getValue().getPayload()).contains("Unsupported message type");
    }

    @Test
    @DisplayName("should decode data URL base64 when audio has prefix")
    void should_decodeDataUrlBase64_when_audioHasPrefix() {
        String encoded = java.util.Base64.getEncoder().encodeToString("wav-bytes".getBytes());
        byte[] decoded = adapter.decodeAudio("data:audio/wav;base64," + encoded);
        assertThat(decoded).isEqualTo("wav-bytes".getBytes());
    }

    @Test
    @DisplayName("should not send when session is closed")
    void should_notSend_when_sessionIsClosed() throws Exception {
        when(session.isOpen()).thenReturn(false);
        StringBuilder transcript = new StringBuilder("hello world");

        adapter.finalizeSession(session, transcript);

        verify(session, never()).sendMessage(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("should send final when session ends with transcript")
    void should_sendFinal_when_sessionEndsWithTranscript() throws Exception {
        when(session.isOpen()).thenReturn(true);
        StringBuilder transcript = new StringBuilder("hello world");

        adapter.finalizeSession(session, transcript);

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        assertThat(captor.getValue().getPayload()).contains("\"type\":\"final\"");
        assertThat(captor.getValue().getPayload()).contains("hello world");
    }
}
