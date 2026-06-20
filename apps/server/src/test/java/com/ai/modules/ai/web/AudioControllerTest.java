package com.ai.modules.ai.web;

import com.ai.modules.ai.application.usecase.TextToSpeechUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AudioController")
@SuppressWarnings("unchecked")
class AudioControllerTest {

    @Mock
    private TextToSpeechUseCase textToSpeechUseCase;

    private AudioController controller;

    @BeforeEach
    void setUp() {
        controller = new AudioController(textToSpeechUseCase);
    }

    @Nested
    @DisplayName("POST /api/audio/speak")
    class Speak {

        @Test
        @DisplayName("should return audio bytes when synthesis successful")
        void shouldReturnAudioBytesWhenSynthesisSuccessful() {
            byte[] audioData = "fake audio content".getBytes();
            when(textToSpeechUseCase.synthesize("Hello world")).thenReturn(audioData);

            var response = controller.speak(new AudioController.TtsRequest("Hello world"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(audioData);
            assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.parseMediaType("audio/mpeg"));
            assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                    .isEqualTo("attachment; filename=\"speech.mp3\"");
            verify(textToSpeechUseCase).synthesize("Hello world");
        }

        @Test
        @DisplayName("should return 500 error when audio is null")
        void shouldReturn500ErrorWhenAudioIsNull() {
            when(textToSpeechUseCase.synthesize(anyString())).thenReturn(null);

            var response = controller.speak(new AudioController.TtsRequest("Test"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNull();
        }

        @Test
        @DisplayName("should return 500 error when audio is empty")
        void shouldReturn500ErrorWhenAudioIsEmpty() {
            when(textToSpeechUseCase.synthesize(anyString())).thenReturn(new byte[0]);

            var response = controller.speak(new AudioController.TtsRequest("Test"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNull();
        }

        @Test
        @DisplayName("should return 500 error when exception is thrown")
        void shouldReturn500ErrorWhenExceptionIsThrown() {
            when(textToSpeechUseCase.synthesize(anyString()))
                    .thenThrow(new RuntimeException("TTS service unavailable"));

            var response = controller.speak(new AudioController.TtsRequest("Test"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNull();
        }
    }

    @Nested
    @DisplayName("GET /api/audio/stream")
    class Stream {

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should return streaming response when successful")
        void shouldReturnStreamingResponseWhenSuccessful() {
            Flux<byte[]> audioStream = Flux.just("chunk1".getBytes(), "chunk2".getBytes());
            when(textToSpeechUseCase.stream(anyString())).thenReturn(audioStream);

            var response = controller.stream("Hello streaming world");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
            assertThat(response.getHeaders().getFirst(HttpHeaders.TRANSFER_ENCODING)).isEqualTo("chunked");
            verify(textToSpeechUseCase).stream("Hello streaming world");
        }

        @Test
        @DisplayName("should return 500 error when stream exception occurs")
        void shouldReturn500ErrorWhenStreamExceptionOccurs() {
            when(textToSpeechUseCase.stream(anyString()))
                    .thenThrow(new RuntimeException("Streaming failed"));

            var response = controller.stream("Test text");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNull();
        }
    }

    @Nested
    @DisplayName("GET /api/audio/voices")
    class GetVoices {

        @Test
        @DisplayName("should return available voices")
        void shouldReturnAvailableVoices() {
            List<String> voices = List.of("alloy", "echo", "nova");
            when(textToSpeechUseCase.getAvailableVoices()).thenReturn(voices);

            var response = controller.getVoices();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            Object voicesObj = response.getBody().get("voices");
            assertThat(voicesObj instanceof List).isTrue();
            @SuppressWarnings("unchecked")
            List<String> resultVoices = (List<String>) voicesObj;
            assertThat(resultVoices).hasSize(3);
            verify(textToSpeechUseCase).getAvailableVoices();
        }

        @Test
        @DisplayName("should return empty list when no voices available")
        void shouldReturnEmptyListWhenNoVoicesAvailable() {
            when(textToSpeechUseCase.getAvailableVoices()).thenReturn(List.of());

            var response = controller.getVoices();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Object voicesObj = response.getBody().get("voices");
            assertThat(voicesObj instanceof List).isTrue();
            @SuppressWarnings("unchecked")
            List<String> resultVoices = (List<String>) voicesObj;
            assertThat(resultVoices).isEmpty();
        }
    }

    @Nested
    @DisplayName("GET /api/audio/models")
    class GetModels {

        @Test
        @DisplayName("should return available models")
        void shouldReturnAvailableModels() {
            List<String> mockModels = List.of("tts-1", "tts-1-hd");
            when(textToSpeechUseCase.getAvailableModels()).thenReturn(mockModels);

            var response = controller.getModels();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            Object modelsObj = response.getBody().get("models");
            assertThat(modelsObj instanceof List).isTrue();
            @SuppressWarnings("unchecked")
            List<String> resultModels = (List<String>) modelsObj;
            assertThat(resultModels).containsExactly("tts-1", "tts-1-hd");
            verify(textToSpeechUseCase).getAvailableModels();
        }
    }
}
