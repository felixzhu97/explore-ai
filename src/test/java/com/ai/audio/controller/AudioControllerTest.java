package com.ai.audio.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ai.audio.domain.model.SynthesizedAudio;
import com.ai.audio.domain.vo.VoiceInfo;
import com.ai.audio.service.AudioService;
import com.ai.common.exception.DomainException;
import com.ai.testsupport.SliceWebMvcTest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SliceWebMvcTest(controllers = AudioController.class)
@DisplayName("AudioController")
class AudioControllerTest {

  @Autowired private MockMvcTester mvc;

  @MockitoBean private AudioService audioService;

  @Nested
  @DisplayName("POST /api/audio/speech")
  class Speak {

    @Test
    @DisplayName("should synthesize speech with valid request")
    void shouldSynthesizeSpeechWithValidRequest() {
      String text = "Hello, world!";
      byte[] audioData = new byte[] {1, 2, 3, 4};
      when(audioService.synthesizeAudio(text, "alloy", 1.0))
          .thenReturn(SynthesizedAudio.create(audioData));

      assertThat(
              mvc.post()
                  .uri("/api/audio/speech")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      """
                      {
                        "text": "Hello, world!",
                        "voice": "alloy",
                        "speed": 1.0,
                        "format": "mp3"
                      }
                      """))
          .hasStatusOk()
          .body()
          .isEqualTo(audioData);

      assertThat(
              mvc.post()
                  .uri("/api/audio/speech")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      """
                      {
                        "text": "Hello, world!",
                        "voice": "alloy",
                        "speed": 1.0,
                        "format": "mp3"
                      }
                      """))
          .hasStatusOk()
          .headers()
          .hasValue(HttpHeaders.CONTENT_TYPE, "audio/mpeg");

      assertThat(
              mvc.post()
                  .uri("/api/audio/speech")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      """
                      {
                        "text": "Hello, world!",
                        "voice": "alloy",
                        "speed": 1.0,
                        "format": "mp3"
                      }
                      """))
          .hasStatusOk()
          .headers()
          .hasValue(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"speech.mp3\"");
    }

    @Test
    @DisplayName("should return 400 for empty request body")
    void shouldReturn400ForEmptyRequestBody() {
      assertThat(
              mvc.post()
                  .uri("/api/audio/speech")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{}"))
          .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("should return 400 for null text")
    void shouldReturn400ForNullText() {
      assertThat(
              mvc.post()
                  .uri("/api/audio/speech")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{}"))
          .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("should return 400 for blank text")
    void shouldReturn400ForBlankText() {
      assertThat(
              mvc.post()
                  .uri("/api/audio/speech")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"text\":\"   \"}"))
          .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("should return 400 for empty text")
    void shouldReturn400ForEmptyText() {
      assertThat(
              mvc.post()
                  .uri("/api/audio/speech")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"text\":\"\"}"))
          .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("should return 500 when audio data is null")
    void shouldReturn500WhenAudioDataIsNull() {
      when(audioService.synthesizeAudio(any(), any(), any())).thenReturn(null);

      assertThat(
              mvc.post()
                  .uri("/api/audio/speech")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"text\":\"Test\"}"))
          .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("should return 500 when audio data is empty")
    void shouldReturn500WhenAudioDataIsEmpty() {
      when(audioService.synthesizeAudio(any(), any(), any())).thenReturn(SynthesizedAudio.empty());

      assertThat(
              mvc.post()
                  .uri("/api/audio/speech")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"text\":\"Test\"}"))
          .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("should return 503 when provider is not configured")
    void shouldReturn503WhenProviderIsNotConfigured() {
      when(audioService.synthesizeAudio(any(), any(), any()))
          .thenThrow(
              DomainException.unavailable(
                  "TTS_PROVIDER_NOT_CONFIGURED",
                  "TTS provider not configured. Set OPENAI_API_KEY or TTS_API_KEY"));

      assertThat(
              mvc.post()
                  .uri("/api/audio/speech")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"text\":\"Test\"}"))
          .hasStatus(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("should return 500 when service throws exception")
    void shouldReturn500WhenServiceThrowsException() {
      when(audioService.synthesizeAudio(any(), any(), any()))
          .thenThrow(new RuntimeException("TTS error"));

      assertThat(
              mvc.post()
                  .uri("/api/audio/speech")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"text\":\"Test\"}"))
          .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("should handle long text without error")
    void shouldHandleLongTextWithoutError() {
      String longText = "A".repeat(10000);
      byte[] audioData = new byte[100];
      when(audioService.synthesizeAudio(longText, null, null))
          .thenReturn(SynthesizedAudio.create(audioData));

      assertThat(
              mvc.post()
                  .uri("/api/audio/speech")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"text\":\"" + longText + "\"}"))
          .hasStatusOk();
    }

    @Test
    @DisplayName("should handle Chinese text")
    void shouldHandleChineseText() {
      String chineseText = "你好，世界！";
      byte[] audioData = new byte[] {1, 2, 3};
      when(audioService.synthesizeAudio(chineseText, null, null))
          .thenReturn(SynthesizedAudio.create(audioData));

      assertThat(
              mvc.post()
                  .uri("/api/audio/speech")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"text\":\"你好，世界！\"}"))
          .hasStatusOk();
    }

    @Test
    @DisplayName("should set correct content type header")
    void shouldSetCorrectContentTypeHeader() {
      byte[] audioData = new byte[] {1, 2, 3};
      when(audioService.synthesizeAudio(any(), any(), any()))
          .thenReturn(SynthesizedAudio.create(audioData));

      assertThat(
              mvc.post()
                  .uri("/api/audio/speech")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"text\":\"Test\"}"))
          .hasStatusOk()
          .headers()
          .hasValue(HttpHeaders.CONTENT_TYPE, "audio/mpeg");
    }

    @Test
    @DisplayName("should set correct content disposition header")
    void shouldSetCorrectContentDispositionHeader() {
      byte[] audioData = new byte[] {1, 2, 3};
      when(audioService.synthesizeAudio(any(), any(), any()))
          .thenReturn(SynthesizedAudio.create(audioData));

      assertThat(
              mvc.post()
                  .uri("/api/audio/speech")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"text\":\"Test\"}"))
          .hasStatusOk()
          .headers()
          .hasValue(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"speech.mp3\"");
    }
  }

  @Nested
  @DisplayName("GET /api/audio/voices")
  class GetVoices {

    @Test
    @DisplayName("should return available voices")
    void shouldReturnAvailableVoices() {
      List<VoiceInfo> voices = List.of(new VoiceInfo("alloy", "Alloy", "en", "neutral"));
      when(audioService.getAvailableVoices()).thenReturn(voices);

      assertThat(mvc.get().uri("/api/audio/voices"))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.voices.length()")
          .convertTo(Integer.class)
          .isEqualTo(1);
    }

    @Test
    @DisplayName("should return empty list when no voices available")
    void shouldReturnEmptyListWhenNoVoicesAvailable() {
      when(audioService.getAvailableVoices()).thenReturn(List.of());

      assertThat(mvc.get().uri("/api/audio/voices"))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.voices.length()")
          .convertTo(Integer.class)
          .isEqualTo(0);
    }
  }

  @Nested
  @DisplayName("GET /api/audio/models")
  class GetTtsModels {

    @Test
    @DisplayName("should return available TTS models")
    void shouldReturnAvailableTtsModels() {
      List<String> models = List.of("tts-1", "tts-1-hd");
      when(audioService.getAvailableTtsModels()).thenReturn(models);

      assertThat(mvc.get().uri("/api/audio/models"))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.models")
          .asArray()
          .containsExactly("tts-1", "tts-1-hd");
    }

    @Test
    @DisplayName("should return empty list when no models available")
    void shouldReturnEmptyListWhenNoModelsAvailable() {
      when(audioService.getAvailableTtsModels()).thenReturn(List.of());

      assertThat(mvc.get().uri("/api/audio/models"))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.models.length()")
          .convertTo(Integer.class)
          .isEqualTo(0);
    }
  }
}
