package com.ai.audio.web;

import com.ai.audio.application.usecase.AudioFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AudioController")
class AudioControllerTest {

    @Mock
    private AudioFacade audioFacade;

    private AudioController controller;

    @BeforeEach
    void setUp() {
        controller = new AudioController(audioFacade);
    }

    @Nested
    @DisplayName("POST /api/audio/speak")
    class Speak {

        @Test
        @DisplayName("should synthesize speech with valid request")
        void shouldSynthesizeSpeechWithValidRequest() {
            String text = "Hello, world!";
            byte[] audioData = new byte[]{1, 2, 3, 4};
            when(audioFacade.synthesize(text)).thenReturn(audioData);

            ResponseEntity<byte[]> response = controller.speak(new AudioController.TtsRequest(text));

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isEqualTo(audioData);
            assertThat(response.getHeaders().getContentType().toString()).contains("audio/mpeg");
            assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                    .isEqualTo("attachment; filename=\"speech.mp3\"");
        }

        @Test
        @DisplayName("should return 400 for null request")
        void shouldReturn400ForNullRequest() {
            ResponseEntity<byte[]> response = controller.speak(null);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody()).isNull();
        }

        @Test
        @DisplayName("should return 400 for null text")
        void shouldReturn400ForNullText() {
            ResponseEntity<byte[]> response = controller.speak(new AudioController.TtsRequest(null));

            assertThat(response.getStatusCode().value()).isEqualTo(400);
        }

        @Test
        @DisplayName("should return 400 for blank text")
        void shouldReturn400ForBlankText() {
            ResponseEntity<byte[]> response = controller.speak(new AudioController.TtsRequest("   "));

            assertThat(response.getStatusCode().value()).isEqualTo(400);
        }

        @Test
        @DisplayName("should return 400 for empty text")
        void shouldReturn400ForEmptyText() {
            ResponseEntity<byte[]> response = controller.speak(new AudioController.TtsRequest(""));

            assertThat(response.getStatusCode().value()).isEqualTo(400);
        }

        @Test
        @DisplayName("should return 500 when audio data is null")
        void shouldReturn500WhenAudioDataIsNull() {
            when(audioFacade.synthesize(any())).thenReturn(null);

            ResponseEntity<byte[]> response = controller.speak(new AudioController.TtsRequest("Test"));

            assertThat(response.getStatusCode().value()).isEqualTo(500);
        }

        @Test
        @DisplayName("should return 500 when audio data is empty")
        void shouldReturn500WhenAudioDataIsEmpty() {
            when(audioFacade.synthesize(any())).thenReturn(new byte[]{});

            ResponseEntity<byte[]> response = controller.speak(new AudioController.TtsRequest("Test"));

            assertThat(response.getStatusCode().value()).isEqualTo(500);
        }

        @Test
        @DisplayName("should return 500 when facade throws exception")
        void shouldReturn500WhenFacadeThrowsException() {
            when(audioFacade.synthesize(any())).thenThrow(new RuntimeException("TTS error"));

            ResponseEntity<byte[]> response = controller.speak(new AudioController.TtsRequest("Test"));

            assertThat(response.getStatusCode().value()).isEqualTo(500);
        }

        @Test
        @DisplayName("should handle long text without error")
        void shouldHandleLongTextWithoutError() {
            String longText = "A".repeat(10000);
            byte[] audioData = new byte[100];
            when(audioFacade.synthesize(longText)).thenReturn(audioData);

            ResponseEntity<byte[]> response = controller.speak(new AudioController.TtsRequest(longText));

            assertThat(response.getStatusCode().value()).isEqualTo(200);
        }

        @Test
        @DisplayName("should handle Chinese text")
        void shouldHandleChineseText() {
            String chineseText = "你好，世界！";
            byte[] audioData = new byte[]{1, 2, 3};
            when(audioFacade.synthesize(chineseText)).thenReturn(audioData);

            ResponseEntity<byte[]> response = controller.speak(new AudioController.TtsRequest(chineseText));

            assertThat(response.getStatusCode().value()).isEqualTo(200);
        }

        @Test
        @DisplayName("should set correct content type header")
        void shouldSetCorrectContentTypeHeader() {
            byte[] audioData = new byte[]{1, 2, 3};
            when(audioFacade.synthesize(any())).thenReturn(audioData);

            ResponseEntity<byte[]> response = controller.speak(new AudioController.TtsRequest("Test"));

            assertThat(response.getHeaders().getContentType().toString())
                    .isEqualTo("audio/mpeg");
        }

        @Test
        @DisplayName("should set correct content disposition header")
        void shouldSetCorrectContentDispositionHeader() {
            byte[] audioData = new byte[]{1, 2, 3};
            when(audioFacade.synthesize(any())).thenReturn(audioData);

            ResponseEntity<byte[]> response = controller.speak(new AudioController.TtsRequest("Test"));

            assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                    .contains("attachment");
            assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                    .contains("speech.mp3");
        }
    }

    @Nested
    @DisplayName("GET /api/audio/voices")
    class GetVoices {

        @Test
        @DisplayName("should return available voices")
        void shouldReturnAvailableVoices() {
            List<String> voices = List.of("alloy", "echo", "fable", "onyx", "nova", "shimmer");
            when(audioFacade.getAvailableVoices()).thenReturn(voices);

            ResponseEntity<Map<String, Object>> response = controller.getVoices();

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).containsEntry("voices", voices);
        }

        @Test
        @DisplayName("should return empty list when no voices available")
        void shouldReturnEmptyListWhenNoVoicesAvailable() {
            when(audioFacade.getAvailableVoices()).thenReturn(List.of());

            ResponseEntity<Map<String, Object>> response = controller.getVoices();

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).containsEntry("voices", List.of());
        }
    }

    @Nested
    @DisplayName("GET /api/audio/models")
    class GetTtsModels {

        @Test
        @DisplayName("should return available TTS models")
        void shouldReturnAvailableTtsModels() {
            List<String> models = List.of("tts-1", "tts-1-hd");
            when(audioFacade.getAvailableTtsModels()).thenReturn(models);

            ResponseEntity<Map<String, Object>> response = controller.getTtsModels();

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).containsEntry("models", models);
        }

        @Test
        @DisplayName("should return empty list when no models available")
        void shouldReturnEmptyListWhenNoModelsAvailable() {
            when(audioFacade.getAvailableTtsModels()).thenReturn(List.of());

            ResponseEntity<Map<String, Object>> response = controller.getTtsModels();

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).containsEntry("models", List.of());
        }
    }
}
